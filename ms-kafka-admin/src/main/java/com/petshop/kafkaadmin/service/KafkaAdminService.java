package com.petshop.kafkaadmin.service;

import com.petshop.kafkaadmin.web.dto.ClusterInfoResponse;
import com.petshop.kafkaadmin.web.dto.ConsumerGroupResponse;
import com.petshop.kafkaadmin.web.dto.NombresKafka;
import com.petshop.kafkaadmin.web.dto.TopicInfoResponse;
import com.petshop.kafkaadmin.web.dto.TopicRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Administracion del cluster Kafka sobre el AdminClient: topicos (crear, listar, describir, cambiar
 * configuracion y particiones, eliminar) y consumer groups (listar, ver offsets y lag, eliminar).
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final AdminClient admin;
    private final int particionesPorDefecto;
    private final short replicasPorDefecto;
    private final Set<String> topicosProtegidos;

    public KafkaAdminService(AdminClient admin,
                             @Value("${app.kafka.default-partitions:3}") int particionesPorDefecto,
                             @Value("${app.kafka.default-replication-factor:3}") short replicasPorDefecto,
                             @Value("${app.kafka.protected-topics:}") String protegidos) {
        this.admin = admin;
        this.particionesPorDefecto = particionesPorDefecto;
        this.replicasPorDefecto = replicasPorDefecto;
        this.topicosProtegidos = Arrays.stream(protegidos.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ topicos

    public List<String> listarTopicos() {
        return espera(admin.listTopics(new ListTopicsOptions().listInternal(false)).names())
                .stream().sorted().toList();
    }

    public TopicInfoResponse infoTopico(String nombre) {
        TopicDescription d = espera(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        List<TopicInfoResponse.PartitionInfo> detalle = d.partitions().stream()
                .map(p -> new TopicInfoResponse.PartitionInfo(
                        p.partition(),
                        p.leader() != null ? p.leader().id() : -1,
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .sorted(Comparator.comparingInt(TopicInfoResponse.PartitionInfo::partition))
                .toList();
        int replicas = detalle.isEmpty() ? 0 : detalle.get(0).replicas().size();
        return new TopicInfoResponse(nombre, detalle.size(), replicas, d.isInternal(),
                configuracionExplicita(nombre), detalle);
    }

    public TopicInfoResponse crearTopico(TopicRequest req) {
        Map<String, String> config = validarConfig(req.config());
        int particiones = req.partitions() != null ? req.partitions() : particionesPorDefecto;
        short replicas = req.replicationFactor() != null ? req.replicationFactor() : replicasPorDefecto;

        int brokers = espera(admin.describeCluster().nodes()).size();
        if (replicas > brokers) {
            throw new IllegalArgumentException("replicationFactor (" + replicas
                    + ") no puede ser mayor que los brokers disponibles (" + brokers + ")");
        }
        espera(admin.createTopics(List.of(new NewTopic(req.name(), particiones, replicas).configs(config))).all());
        log.info("Topico creado: {} (particiones={}, replicas={}, config={})", req.name(), particiones, replicas, config);
        return infoTopico(req.name());
    }

    /** Cambia configuraciones del topico (retention.ms, cleanup.policy, ...). Solo claves permitidas. */
    public TopicInfoResponse actualizarConfig(String nombre, Map<String, String> cambios) {
        Map<String, String> config = validarConfig(cambios);
        infoTopico(nombre); // 404 si no existe
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> ops = config.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        espera(admin.incrementalAlterConfigs(Map.of(recurso, (Collection<AlterConfigOp>) ops)).all());
        log.info("Configuracion actualizada en {}: {}", nombre, config);
        return infoTopico(nombre);
    }

    /** Aumenta el numero de particiones (Kafka no permite reducirlas). */
    public TopicInfoResponse aumentarParticiones(String nombre, int total) {
        int actuales = infoTopico(nombre).partitions();
        if (total <= actuales) {
            throw new IllegalArgumentException("Solo se pueden aumentar las particiones: el topico tiene "
                    + actuales + " y se pidieron " + total);
        }
        espera(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Particiones de {} aumentadas de {} a {}", nombre, actuales, total);
        return infoTopicoConParticiones(nombre, total);
    }

    /** El cluster tarda un instante en propagar las particiones nuevas: se espera (hasta ~3 s) a verlas en la descripcion. */
    private TopicInfoResponse infoTopicoConParticiones(String nombre, int esperadas) {
        TopicInfoResponse info = infoTopico(nombre);
        for (int intento = 0; intento < 10 && info.partitions() < esperadas; intento++) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            info = infoTopico(nombre);
        }
        return info;
    }

    public void eliminarTopico(String nombre) {
        if (topicosProtegidos.contains(nombre)) {
            throw new OperacionNoPermitidaException("El topico '" + nombre + "' es parte del sistema y no se puede eliminar");
        }
        infoTopico(nombre); // 404 si no existe
        espera(admin.deleteTopics(List.of(nombre)).all());
        log.info("Topico eliminado: {}", nombre);
    }

    // ------------------------------------------------------------ consumer groups

    public List<String> listarGrupos() {
        return espera(admin.listConsumerGroups().all()).stream()
                .map(g -> g.groupId()).sorted().toList();
    }

    /** Estado del grupo y lag por particion: lag = offset final del topico - offset confirmado por el grupo. */
    public ConsumerGroupResponse infoGrupo(String groupId) {
        ConsumerGroupDescription d = espera(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                espera(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        if (confirmados.isEmpty() && d.members().isEmpty() && d.state().toString().equals("DEAD")) {
            throw new GroupIdNotFoundException("El consumer group '" + groupId + "' no existe");
        }

        // Particiones del grupo = las que ya tienen offset confirmado + las asignadas a sus consumidores
        // (una particion asignada que aun no recibio mensajes no tiene offset confirmado, pero igual es del grupo).
        Set<TopicPartition> particiones = new java.util.HashSet<>(confirmados.keySet());
        d.members().forEach(m -> particiones.addAll(m.assignment().topicPartitions()));

        Map<TopicPartition, ListOffsetsResultInfo> finales = particiones.isEmpty() ? Map.of()
                : espera(admin.listOffsets(particiones.stream()
                        .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()))).all());
        Map<TopicPartition, ListOffsetsResultInfo> iniciales = particiones.isEmpty() ? Map.of()
                : espera(admin.listOffsets(particiones.stream()
                        .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.earliest()))).all());

        List<TopicPartition> ordenadas = new ArrayList<>(particiones);
        ordenadas.sort(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition));

        List<ConsumerGroupResponse.PartitionLag> detalle = new ArrayList<>();
        long lagTotal = 0;
        for (TopicPartition tp : ordenadas) {
            OffsetAndMetadata confirmado = confirmados.get(tp);
            // Sin offset confirmado el grupo empezaria desde el primer mensaje que aun conserva el topico.
            long actual = confirmado != null ? confirmado.offset() : iniciales.get(tp).offset();
            long fin = finales.get(tp).offset();
            long lag = Math.max(0, fin - actual);
            lagTotal += lag;
            detalle.add(new ConsumerGroupResponse.PartitionLag(tp.topic(), tp.partition(), actual, fin, lag));
        }
        return new ConsumerGroupResponse(groupId, d.state().toString(), d.members().size(), lagTotal, detalle);
    }

    /** Elimina un grupo SIN miembros activos (si tiene consumidores conectados, Kafka lo rechaza). */
    public void eliminarGrupo(String groupId) {
        espera(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.info("Consumer group eliminado: {}", groupId);
    }

    // ------------------------------------------------------------------- cluster

    public ClusterInfoResponse infoCluster() {
        var cluster = admin.describeCluster();
        String id = espera(cluster.clusterId());
        Node controlador = espera(cluster.controller());
        List<ClusterInfoResponse.Broker> brokers = espera(cluster.nodes()).stream()
                .map(n -> new ClusterInfoResponse.Broker(n.id(), n.host(), n.port()))
                .sorted(Comparator.comparingInt(ClusterInfoResponse.Broker::id))
                .toList();
        return new ClusterInfoResponse(id, controlador != null ? controlador.id() : -1, brokers);
    }

    // ------------------------------------------------------------------- apoyo

    /** Solo las configuraciones puestas explicitamente en el topico (no las heredadas del broker). */
    private Map<String, String> configuracionExplicita(String nombre) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = espera(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        Map<String, String> explicitas = new TreeMap<>();
        for (ConfigEntry entrada : config.entries()) {
            if (entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                explicitas.put(entrada.name(), entrada.value());
            }
        }
        return explicitas;
    }

    private static Map<String, String> validarConfig(Map<String, String> config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, String> limpia = new HashMap<>();
        for (Map.Entry<String, String> e : config.entrySet()) {
            if (!NombresKafka.CONFIGS_PERMITIDAS.contains(e.getKey())) {
                throw new IllegalArgumentException("Configuracion no permitida: '" + e.getKey()
                        + "'. Permitidas: " + new TreeSet<>(NombresKafka.CONFIGS_PERMITIDAS));
            }
            if (e.getValue() == null || e.getValue().isBlank()) {
                throw new IllegalArgumentException("La configuracion '" + e.getKey() + "' no puede estar vacia");
            }
            limpia.put(e.getKey(), e.getValue().trim());
        }
        return limpia;
    }

    /** Espera el resultado de una operacion Kafka y propaga la causa real (no la ExecutionException). */
    private static <T> T espera(KafkaFuture<T> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operacion interrumpida", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw new IllegalStateException(e.getCause());
        }
    }
}

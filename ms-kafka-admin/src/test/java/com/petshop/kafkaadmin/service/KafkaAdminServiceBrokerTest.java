package com.petshop.kafkaadmin.service;

import com.petshop.kafkaadmin.web.dto.ConsumerGroupResponse;
import com.petshop.kafkaadmin.web.dto.TopicInfoResponse;
import com.petshop.kafkaadmin.web.dto.TopicRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el servicio contra un broker Kafka real (embebido): crear, describir, modificar, eliminar y medir el lag. */
@SpringBootTest(properties = {
        "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.default-replication-factor=1",
        "app.kafka.protected-topics=topico-protegido"
})
@EmbeddedKafka(partitions = 1)
@ActiveProfiles("test")
class KafkaAdminServiceBrokerTest {

    @Autowired
    private KafkaAdminService service;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    @Test
    void creaUnTopicoConConfiguracionYLoDescribe() {
        TopicInfoResponse t = service.crearTopico(new TopicRequest("t-crear", 3, (short) 1,
                Map.of("retention.ms", "86400000", "cleanup.policy", "delete")));

        assertEquals("t-crear", t.name());
        assertEquals(3, t.partitions());
        assertEquals(1, t.replicationFactor());
        assertEquals("86400000", t.config().get("retention.ms"));
        assertEquals("delete", t.config().get("cleanup.policy"));
        assertEquals(3, t.partitionsDetail().size());
        assertTrue(service.listarTopicos().contains("t-crear"));
    }

    @Test
    void usaLosValoresPorDefectoSiNoSeIndicanParticiones() {
        TopicInfoResponse t = service.crearTopico(new TopicRequest("t-defecto", null, null, null));

        assertEquals(3, t.partitions());
        assertEquals(1, t.replicationFactor()); // default de prueba: 1 broker
    }

    @Test
    void noPermiteMasReplicasQueBrokers() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearTopico(new TopicRequest("t-rf", 1, (short) 3, null)));
        assertTrue(ex.getMessage().contains("brokers disponibles"));
    }

    @Test
    void rechazaCrearUnTopicoQueYaExiste() {
        service.crearTopico(new TopicRequest("t-dup", 1, (short) 1, null));
        assertThrows(TopicExistsException.class,
                () -> service.crearTopico(new TopicRequest("t-dup", 1, (short) 1, null)));
    }

    @Test
    void actualizaLaConfiguracionYAumentaParticiones() {
        service.crearTopico(new TopicRequest("t-mod", 1, (short) 1, null));

        TopicInfoResponse conConfig = service.actualizarConfig("t-mod", Map.of("retention.ms", "3600000"));
        assertEquals("3600000", conConfig.config().get("retention.ms"));

        TopicInfoResponse conParticiones = service.aumentarParticiones("t-mod", 4);
        assertEquals(4, conParticiones.partitions());

        assertThrows(IllegalArgumentException.class, () -> service.aumentarParticiones("t-mod", 2));
    }

    @Test
    void rechazaConfiguracionesFueraDeLaListaPermitida() {
        service.crearTopico(new TopicRequest("t-cfg", 1, (short) 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarConfig("t-cfg", Map.of("unclean.leader.election.enable", "true")));
    }

    @Test
    void eliminaUnTopicoPeroNoLosProtegidos() {
        service.crearTopico(new TopicRequest("t-borrar", 1, (short) 1, null));
        service.eliminarTopico("t-borrar");
        assertThrows(UnknownTopicOrPartitionException.class, () -> service.infoTopico("t-borrar"));

        service.crearTopico(new TopicRequest("topico-protegido", 1, (short) 1, null));
        assertThrows(OperacionNoPermitidaException.class, () -> service.eliminarTopico("topico-protegido"));
        assertTrue(service.listarTopicos().contains("topico-protegido"));
    }

    @Test
    void informaElClusterYMideElLagDeUnConsumerGroup() {
        assertEquals(1, service.infoCluster().brokers().size());

        service.crearTopico(new TopicRequest("t-lag", 1, (short) 1, null));
        publicar("t-lag", 5);
        consumirYConfirmar("t-lag", "grupo-lag", 5);
        publicar("t-lag", 3); // quedan 3 mensajes sin consumir

        ConsumerGroupResponse grupo = service.infoGrupo("grupo-lag");
        assertEquals("grupo-lag", grupo.groupId());
        assertEquals(3, grupo.lagTotal());
        assertEquals(5, grupo.partitions().get(0).currentOffset());
        assertEquals(8, grupo.partitions().get(0).endOffset());
        assertTrue(service.listarGrupos().contains("grupo-lag"));
    }

    @Test
    void unGrupoActivoMuestraTodasSusParticionesAsignadasAunqueNoHayaConfirmadoOffsets() {
        service.crearTopico(new TopicRequest("t-asig", 3, (short) 1, null));
        publicar("t-asig", 1);

        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ConsumerConfig.GROUP_ID_CONFIG, "grupo-asig",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            c.subscribe(List.of("t-asig"));
            long limite = System.currentTimeMillis() + 15_000;
            while (c.assignment().size() < 3 && System.currentTimeMillis() < limite) {
                c.poll(Duration.ofMillis(200));
            }

            ConsumerGroupResponse grupo = service.infoGrupo("grupo-asig");

            assertEquals(1, grupo.members());
            assertEquals(3, grupo.partitions().size(), "las 3 particiones asignadas, tengan o no offset confirmado");
            assertEquals(1, grupo.lagTotal(), "el mensaje publicado aun no fue confirmado por el grupo");
        }
    }

    private void publicar(String topico, int cantidad) {
        try (KafkaProducer<String, String> p = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            for (int i = 0; i < cantidad; i++) {
                p.send(new ProducerRecord<>(topico, "k" + i, "v" + i));
            }
            p.flush();
        }
    }

    private void consumirYConfirmar(String topico, String grupo, int esperados) {
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ConsumerConfig.GROUP_ID_CONFIG, grupo,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            c.subscribe(List.of(topico));
            int leidos = 0;
            long limite = System.currentTimeMillis() + 15_000;
            while (leidos < esperados && System.currentTimeMillis() < limite) {
                leidos += c.poll(Duration.ofMillis(500)).count();
            }
            assertEquals(esperados, leidos);
            c.commitSync();
        }
    }
}

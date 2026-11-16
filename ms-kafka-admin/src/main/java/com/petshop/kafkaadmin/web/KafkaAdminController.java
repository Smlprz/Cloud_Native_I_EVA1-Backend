package com.petshop.kafkaadmin.web;

import com.petshop.kafkaadmin.service.KafkaAdminService;
import com.petshop.kafkaadmin.web.dto.ClusterInfoResponse;
import com.petshop.kafkaadmin.web.dto.ConsumerGroupResponse;
import com.petshop.kafkaadmin.web.dto.NombresKafka;
import com.petshop.kafkaadmin.web.dto.PartitionsRequest;
import com.petshop.kafkaadmin.web.dto.TopicConfigRequest;
import com.petshop.kafkaadmin.web.dto.TopicInfoResponse;
import com.petshop.kafkaadmin.web.dto.TopicRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administracion de Kafka. Exige JWT de Azure AD con rol ADMIN y scope kafka.admin
 * (o carrito.write mientras ese scope no exista en el App Registration), igual que el administrador de RabbitMQ.
 */
@RestController
@RequestMapping("/api/kafka")
@PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('SCOPE_kafka.admin','SCOPE_carrito.write')")
public class KafkaAdminController {

    private static final String NOMBRE = NombresKafka.NOMBRE_REGEX;

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    // ---- cluster

    @GetMapping("/cluster")
    public ClusterInfoResponse cluster() {
        return service.infoCluster();
    }

    // ---- topicos

    @GetMapping("/topics")
    public List<String> listarTopicos() {
        return service.listarTopicos();
    }

    @GetMapping("/topics/{name}")
    public TopicInfoResponse infoTopico(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String name) {
        return service.infoTopico(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicInfoResponse crearTopico(@Valid @RequestBody TopicRequest request) {
        return service.crearTopico(request);
    }

    @PutMapping("/topics/{name}/config")
    public TopicInfoResponse actualizarConfig(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String name,
                                              @Valid @RequestBody TopicConfigRequest request) {
        return service.actualizarConfig(name, request.config());
    }

    @PutMapping("/topics/{name}/partitions")
    public TopicInfoResponse aumentarParticiones(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String name,
                                                 @Valid @RequestBody PartitionsRequest request) {
        return service.aumentarParticiones(name, request.total());
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTopico(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String name) {
        service.eliminarTopico(name);
    }

    // ---- consumer groups

    @GetMapping("/consumer-groups")
    public List<String> listarGrupos() {
        return service.listarGrupos();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupResponse infoGrupo(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String groupId) {
        return service.infoGrupo(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarGrupo(@PathVariable @Size(max = NombresKafka.MAX) @Pattern(regexp = NOMBRE, message = NombresKafka.NOMBRE_MENSAJE) String groupId) {
        service.eliminarGrupo(groupId);
    }
}

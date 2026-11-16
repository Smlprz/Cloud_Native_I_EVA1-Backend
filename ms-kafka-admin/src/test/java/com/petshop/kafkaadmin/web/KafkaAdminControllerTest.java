package com.petshop.kafkaadmin.web;

import com.petshop.kafkaadmin.service.KafkaAdminService;
import com.petshop.kafkaadmin.service.OperacionNoPermitidaException;
import com.petshop.kafkaadmin.web.dto.TopicInfoResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private KafkaAdminService service;

    @MockBean
    private AdminClient adminClient; // evita abrir conexion real a Kafka

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_kafka.admin"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor cliente() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_kafka.admin"), new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/kafka/topics")).andExpect(status().isUnauthorized());
    }

    @Test
    void unClienteNoPuedeAdministrarKafka() throws Exception {
        mvc.perform(get("/api/kafka/topics").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/kafka/topics/eventos.pagos").with(cliente())).andExpect(status().isForbidden());
    }

    @Test
    void elAdminListaLosTopicos() throws Exception {
        when(service.listarTopicos()).thenReturn(List.of("eventos.navegacion", "eventos.pagos"));

        mvc.perform(get("/api/kafka/topics").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("eventos.navegacion"));
    }

    @Test
    void creaUnTopicoValido() throws Exception {
        when(service.crearTopico(any())).thenReturn(
                new TopicInfoResponse("nuevo", 3, 3, false, Map.of("retention.ms", "1000"), List.of()));

        mvc.perform(post("/api/kafka/topics").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nuevo\",\"partitions\":3,\"replicationFactor\":3,\"config\":{\"retention.ms\":\"1000\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("nuevo"));
    }

    @Test
    void rechazaDatosInvalidosConUn400() throws Exception {
        mvc.perform(post("/api/kafka/topics").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nombre raro!\",\"partitions\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/kafka/topics").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partitions\":3}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/kafka/topics/nombre%20raro").with(admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/kafka/topics/x/config").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"config\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void traduceLosErroresDeKafkaACodigosHttp() throws Exception {
        when(service.infoTopico("no-existe")).thenThrow(new UnknownTopicOrPartitionException("no-existe"));
        mvc.perform(get("/api/kafka/topics/no-existe").with(admin())).andExpect(status().isNotFound());

        doThrow(new OperacionNoPermitidaException("es del sistema")).when(service).eliminarTopico("eventos.pagos");
        mvc.perform(delete("/api/kafka/topics/eventos.pagos").with(admin())).andExpect(status().isConflict());

        when(service.listarGrupos()).thenThrow(new TimeoutException("sin respuesta"));
        mvc.perform(get("/api/kafka/consumer-groups").with(admin())).andExpect(status().isServiceUnavailable());
    }

    @Test
    void eliminaUnTopico() throws Exception {
        mvc.perform(delete("/api/kafka/topics/mi-topico").with(admin())).andExpect(status().isNoContent());
        verify(service).eliminarTopico("mi-topico");
    }
}

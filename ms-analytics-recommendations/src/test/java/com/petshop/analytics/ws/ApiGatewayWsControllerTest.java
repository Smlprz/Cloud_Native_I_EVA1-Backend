package com.petshop.analytics.ws;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** $connect del API Gateway WebSocket: sin token valido la conexion se rechaza y no queda registrada. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiGatewayWsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiGatewayWsDifusor difusor;

    @Test
    void sinTokenSeRechazaLaConexion() throws Exception {
        mvc.perform(post("/ws/gateway/conectar").header("connectionId", "abc=")).andExpect(status().isUnauthorized());
        assertThat(difusor.conectados()).isZero();
    }

    @Test
    void conTokenInvalidoSeRechazaLaConexion() throws Exception {
        mvc.perform(post("/ws/gateway/conectar").header("connectionId", "abc=").param("token", "no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
        assertThat(difusor.conectados()).isZero();
    }

    @Test
    void desconectarEsIdempotente() throws Exception {
        mvc.perform(post("/ws/gateway/desconectar").header("connectionId", "xyz=")).andExpect(status().isOk());
    }
}

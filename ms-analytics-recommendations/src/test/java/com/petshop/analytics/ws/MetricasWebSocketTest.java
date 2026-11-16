package com.petshop.analytics.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.analytics.Eventos;
import com.petshop.analytics.service.MetricasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Dashboard en tiempo real: el WebSocket exige un JWT de administrador y envia las metricas cada vez que cambian. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MetricasWebSocketTest {

    @LocalServerPort private int puerto;
    @Autowired private MetricasService metricas;
    @MockBean private JwtDecoder jwtDecoder;

    private final ObjectMapper mapper = new ObjectMapper();

    private static Jwt jwtConRol(String rol) {
        return Jwt.withTokenValue("t").header("alg", "none").subject("usuario").claim("roles", List.of(rol)).build();
    }

    @BeforeEach
    void tokens() {
        when(jwtDecoder.decode("token-admin")).thenReturn(jwtConRol("Administrador"));
        when(jwtDecoder.decode("token-cliente")).thenReturn(jwtConRol("Cliente"));
        when(jwtDecoder.decode("token-malo")).thenThrow(new BadJwtException("firma invalida"));
        when(jwtDecoder.decode("")).thenThrow(new BadJwtException("vacio"));
    }

    private BlockingQueue<String> conectar(String query) throws Exception {
        BlockingQueue<String> mensajes = new LinkedBlockingQueue<>();
        new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                mensajes.add(message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                // nada
            }
        }, "ws://localhost:" + puerto + "/ws/metricas" + query).get(10, TimeUnit.SECONDS);
        return mensajes;
    }

    @Test
    void unAdminRecibeLasMetricasAlConectarseYCuandoCambian() throws Exception {
        BlockingQueue<String> mensajes = conectar("?token=token-admin");

        String inicial = mensajes.poll(10, TimeUnit.SECONDS);
        assertNotNull(inicial, "debe recibir la foto inicial al conectarse");
        JsonNode primera = mapper.readTree(inicial);
        assertTrue(primera.has("totalEventos") && primera.has("pagos") && primera.has("productosMasVistos"));
        long base = primera.get("totalEventos").asLong();

        metricas.registrar(Eventos.visto("ws-user", 1L, "Gatos")); // llega un evento nuevo

        long recibido = -1;
        long limite = System.currentTimeMillis() + 8_000;
        while (System.currentTimeMillis() < limite && recibido < base + 1) {
            String m = mensajes.poll(1, TimeUnit.SECONDS);
            if (m != null) {
                recibido = mapper.readTree(m).get("totalEventos").asLong();
            }
        }
        assertEquals(base + 1, recibido, "el dashboard debe recibir la actualizacion sin pedirla");
    }

    @Test
    void sinTokenLaConexionSeRechaza() {
        assertThrows(ExecutionException.class, () -> conectar(""));
    }

    @Test
    void conUnTokenInvalidoLaConexionSeRechaza() {
        assertThrows(ExecutionException.class, () -> conectar("?token=token-malo"));
    }

    @Test
    void unClienteQueNoEsAdminNoPuedeConectarse() {
        assertThrows(ExecutionException.class, () -> conectar("?token=token-cliente"));
    }
}

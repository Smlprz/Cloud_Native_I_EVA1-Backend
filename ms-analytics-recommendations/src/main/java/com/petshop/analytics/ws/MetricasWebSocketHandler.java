package com.petshop.analytics.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petshop.analytics.service.MetricasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal WebSocket /ws/metricas: envia la foto de las metricas al conectarse y cada vez que cambian.
 * Es solo de salida (el dashboard no manda nada). Solo se conectan administradores (ver JwtHandshakeInterceptor).
 */
@Component
public class MetricasWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricasWebSocketHandler.class);

    private final MetricasService metricas;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();

    public MetricasWebSocketHandler(MetricasService metricas) {
        this.metricas = metricas;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // El decorador hace segura la escritura concurrente (difusion + envio inicial).
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024);
        sesiones.put(session.getId(), segura);
        log.info("Dashboard conectado ({} sesion(es))", sesiones.size());
        enviar(segura, json());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session.getId());
        log.info("Dashboard desconectado ({} sesion(es))", sesiones.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) {
        log.debug("Error de transporte WebSocket: {}", error.getMessage());
        sesiones.remove(session.getId());
    }

    public int conectados() {
        return sesiones.size();
    }

    /** Envia la foto actual de las metricas a todos los dashboards conectados. */
    public void difundir() {
        if (sesiones.isEmpty()) {
            return;
        }
        String mensaje = json();
        sesiones.values().forEach(s -> enviar(s, mensaje));
    }

    private void enviar(WebSocketSession sesion, String mensaje) {
        try {
            if (sesion.isOpen()) {
                sesion.sendMessage(new TextMessage(mensaje));
            }
        } catch (IOException | RuntimeException e) {
            log.debug("No se pudo enviar a la sesion {}: {}", sesion.getId(), e.getMessage());
            sesiones.remove(sesion.getId());
            try {
                sesion.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ignorado) {
                // la sesion ya estaba cerrada
            }
        }
    }

    private String json() {
        try {
            return mapper.writeValueAsString(metricas.snapshot());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron serializar las metricas", e);
        }
    }
}

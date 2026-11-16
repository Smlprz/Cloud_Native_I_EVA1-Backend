package com.petshop.analytics.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/** Registra el canal /ws/metricas (con validacion de JWT y control de origen) y activa las tareas programadas. */
@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    private final MetricasWebSocketHandler handler;
    private final JwtHandshakeInterceptor interceptor;
    private final List<String> origenesPermitidos;

    public WebSocketConfig(MetricasWebSocketHandler handler, JwtHandshakeInterceptor interceptor,
                           @Value("${security.cors.allowed-origins}") List<String> origenesPermitidos) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/metricas")
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns(origenesPermitidos.toArray(new String[0]));
    }
}

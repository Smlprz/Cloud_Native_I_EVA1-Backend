package com.petshop.productos.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Contrato de mensajeria de Pedidos360, leido desde application.yml (prefijo "messaging").
 * Es la unica fuente de nombres de exchanges, colas, DLQ y routing keys: ninguna clase
 * de negocio escribe un nombre a mano. Es identico en todos los microservicios.
 */
@ConfigurationProperties(prefix = "messaging")
public record MessagingProperties(
        Exchanges exchanges,
        Queues queues,
        Dlqs dlqs,
        RoutingKeys routingKeys,
        Retry retry) {

    /** direct = ordenes y comandos, topic = eventos de inventario, dlx = mensajes muertos (direct). */
    public record Exchanges(String direct, String topic, String dlx) {
    }

    public record Queues(String ordenCreada, String stockDescontar, String stockBajo, String resenaCreada) {
    }

    /** Cola de mensajes muertos de cada cola. Su nombre es tambien la routing key en el DLX. */
    public record Dlqs(String ordenCreada, String stockDescontar, String stockBajo, String resenaCreada) {
    }

    public record RoutingKeys(String ordenCreada, String stockDescontar, String stockBajo, String resenaCreada) {
    }

    /** Reintentos locales de un consumidor antes de enviar el mensaje a la DLQ. */
    public record Retry(int maxAttempts, long backoffMs) {
    }
}

package com.petshop.cupones.messaging.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Publica cupon.usado (RabbitMQ, punto a punto) cuando un cupon se aplica en una compra; lo consume
 * ms-analytics-recommendations para contar los cupones usados. Si RabbitMQ falla, solo se registra el error.
 */
@Component
public class CuponUsadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(CuponUsadoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public CuponUsadoPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicar(String codigo, String usuarioId, BigDecimal descuento) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().cuponUsado(),
                    new CuponUsadoEvent(codigo, usuarioId, descuento));
            log.info("Publicado '{}' del cupon {}", props.routingKeys().cuponUsado(), codigo);
        } catch (Exception e) {
            log.error("No se pudo publicar '{}' del cupon {}: {}", props.routingKeys().cuponUsado(), codigo, e.getMessage());
        }
    }

    public record CuponUsadoEvent(String codigo, String usuarioId, BigDecimal descuento) {
    }
}

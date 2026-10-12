package com.petshop.orden.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica el evento "orden creada". Si RabbitMQ falla, la orden ya guardada no se pierde: se registra el error. */
@Component
public class OrdenPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrdenPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrdenPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(OrdenCreadaEvent evento) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, evento);
            log.info("Evento orden.creada publicado para la orden {}", evento.ordenId());
        } catch (Exception e) {
            log.error("No se pudo publicar el evento de la orden {}: {}", evento.ordenId(), e.getMessage());
        }
    }
}

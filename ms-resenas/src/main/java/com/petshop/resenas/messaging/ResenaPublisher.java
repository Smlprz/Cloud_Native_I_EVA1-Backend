package com.petshop.resenas.messaging;

import com.petshop.resenas.messaging.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica resena.creada en el exchange direct. Si RabbitMQ falla, la resena ya guardada
 * no se pierde: se registra el error.
 */
@Component
public class ResenaPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResenaPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public ResenaPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicar(ResenaCreadaEvent evento) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().resenaCreada(), evento);
            log.info("Publicado '{}' de la resena {}", props.routingKeys().resenaCreada(), evento.resenaId());
        } catch (Exception e) {
            log.error("No se pudo publicar '{}' de la resena {}: {}",
                    props.routingKeys().resenaCreada(), evento.resenaId(), e.getMessage());
        }
    }
}

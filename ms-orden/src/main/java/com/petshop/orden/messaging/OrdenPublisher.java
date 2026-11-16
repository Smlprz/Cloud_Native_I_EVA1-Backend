package com.petshop.orden.messaging;

import com.petshop.orden.messaging.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica lo que ocurre al crear una orden, en el exchange direct:
 *  - orden.creada    -> ms-notificaciones envia el correo de confirmacion.
 *  - stock.descontar -> ms-productos descuenta el stock.
 * Si RabbitMQ falla, la orden ya guardada no se pierde: se registra el error.
 */
@Component
public class OrdenPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrdenPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public OrdenPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicar(OrdenCreadaEvent evento) {
        enviar(props.routingKeys().ordenCreada(), evento, evento.ordenId());
    }

    public void publicarDescuentoDeStock(StockDescontarCommand comando) {
        enviar(props.routingKeys().stockDescontar(), comando, comando.ordenId());
    }

    private void enviar(String routingKey, Object mensaje, Long ordenId) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().direct(), routingKey, mensaje);
            log.info("Publicado '{}' de la orden {}", routingKey, ordenId);
        } catch (Exception e) {
            log.error("No se pudo publicar '{}' de la orden {}: {}", routingKey, ordenId, e.getMessage());
        }
    }
}

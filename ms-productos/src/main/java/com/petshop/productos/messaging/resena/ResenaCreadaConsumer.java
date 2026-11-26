package com.petshop.productos.messaging.resena;

import com.petshop.productos.messaging.config.MessagingProperties;
import com.petshop.productos.messaging.event.ResenaCreadaEvent;
import com.petshop.productos.messaging.support.AckSupport;
import com.petshop.productos.messaging.support.EventParser;
import com.petshop.productos.messaging.support.NonRetryableMessageException;
import com.petshop.productos.service.CalificacionService;
import com.petshop.productos.service.ProductoInexistenteException;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de resena.creada (cola resena.creada). Actualiza la calificacion promedio del
 * producto. Si el producto no existe, el mensaje va a resena.creada.dlq sin reintentos.
 */
@Component
public class ResenaCreadaConsumer {

    static final String LISTENER_ID = "resena-creada-listener";

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final CalificacionService calificacionService;
    private final MessagingProperties props;

    public ResenaCreadaConsumer(EventParser parser, AckSupport ackSupport,
                                CalificacionService calificacionService, MessagingProperties props) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.calificacionService = calificacionService;
        this.props = props;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.resena-creada}")
    public void onResenaCreada(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().resenaCreada(), message, channel, () -> {
            ResenaCreadaEvent evento = parser.parse(message, ResenaCreadaEvent.class);
            try {
                calificacionService.registrar(evento.resenaId(), evento.productoId(), evento.puntaje());
            } catch (ProductoInexistenteException e) {
                throw new NonRetryableMessageException(e.getMessage(), e);
            }
        });
    }
}

package com.petshop.orden.messaging;

import com.petshop.orden.messaging.config.MessagingProperties;
import com.petshop.orden.messaging.support.AckSupport;
import com.petshop.orden.messaging.support.EventParser;
import com.petshop.orden.messaging.support.NonRetryableMessageException;
import com.petshop.orden.service.OrdenService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de pago.procesado: la orden queda PAGADA (pago aprobado) o PAGO_RECHAZADO.
 * Una orden inexistente es error de negocio: va a pago.procesado.dlq sin reintentos.
 */
@Component
public class PagoProcesadoConsumer {

    static final String LISTENER_ID = "pago-procesado-listener";

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final OrdenService ordenService;
    private final MessagingProperties props;

    public PagoProcesadoConsumer(EventParser parser, AckSupport ackSupport, OrdenService ordenService,
                                 MessagingProperties props) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.ordenService = ordenService;
        this.props = props;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.pago-procesado}")
    public void onPagoProcesado(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().pagoProcesado(), message, channel, () -> {
            PagoProcesadoEvent evento = parser.parse(message, PagoProcesadoEvent.class);
            if (!ordenService.registrarPago(evento.ordenId(), evento.estado())) {
                throw new NonRetryableMessageException("Orden " + evento.ordenId() + " inexistente");
            }
        });
    }
}

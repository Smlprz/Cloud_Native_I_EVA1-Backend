package com.petshop.email.messaging.orden;

import com.petshop.email.messaging.config.MessagingProperties;
import com.petshop.email.messaging.support.AckSupport;
import com.petshop.email.messaging.support.EventParser;
import com.petshop.email.service.EmailService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de orden.creada: envia el correo de confirmacion de la compra.
 * La confirmacion del mensaje (ACK / reintento / DLQ) la gestiona {@link AckSupport}.
 */
@Component
public class OrdenCreadaListener {

    static final String LISTENER_ID = "orden-creada-listener";

    private static final Logger log = LoggerFactory.getLogger(OrdenCreadaListener.class);

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final EmailService emailService;
    private final MessagingProperties props;

    public OrdenCreadaListener(EventParser parser, AckSupport ackSupport,
                               EmailService emailService, MessagingProperties props) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.emailService = emailService;
        this.props = props;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.orden-creada}")
    public void onOrdenCreada(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().ordenCreada(), message, channel, () -> {
            OrdenCreadaEvent evento = parser.parse(message, OrdenCreadaEvent.class);
            log.info("Orden {} recibida desde la cola; enviando correo a {}", evento.ordenId(), evento.email());
            emailService.enviarCorreo(
                    evento.email(),
                    "Confirmacion de tu pedido N° " + evento.ordenId(),
                    "Gracias por tu compra en Pet Shop.\n\nPedido N° " + evento.ordenId()
                            + "\nTotal: $" + evento.total());
        });
    }
}

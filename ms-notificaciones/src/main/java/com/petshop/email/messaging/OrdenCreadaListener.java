package com.petshop.email.messaging;

import com.petshop.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidor: envia el correo de confirmacion y registra en log los mensajes que terminan en la DLQ. */
@Component
public class OrdenCreadaListener {

    private static final Logger log = LoggerFactory.getLogger(OrdenCreadaListener.class);

    private final EmailService emailService;

    public OrdenCreadaListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /** Si lanza excepcion, Spring reintenta y luego rechaza el mensaje: RabbitMQ lo manda a la DLQ. */
    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrdenCreada(OrdenCreadaEvent evento) {
        log.info("Orden {} recibida desde la cola; enviando correo a {}", evento.ordenId(), evento.email());
        emailService.enviarCorreo(
                evento.email(),
                "Confirmacion de tu pedido N° " + evento.ordenId(),
                "Gracias por tu compra en Pet Shop.\n\nPedido N° " + evento.ordenId()
                        + "\nTotal: $" + evento.total());
    }

    @RabbitListener(queues = RabbitConfig.DLQ)
    public void onMensajeMuerto(Message mensaje) {
        log.error("Mensaje NO entregado, movido a la DLQ {}: cuerpo={} motivo={}",
                RabbitConfig.DLQ,
                new String(mensaje.getBody()),
                mensaje.getMessageProperties().getHeaders().get("x-death"));
    }
}

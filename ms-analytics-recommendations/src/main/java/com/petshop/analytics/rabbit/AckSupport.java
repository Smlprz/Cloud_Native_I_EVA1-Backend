package com.petshop.analytics.rabbit;


import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Confirmacion explicita (ACK manual) de los mensajes. Politica unica para los consumidores:
 *
 *   exito                         -> basicAck
 *   NonRetryableMessageException  -> basicNack(requeue=false)  => DLQ, sin reintentos
 *   otro error                    -> se reintenta hasta messaging.retry.max-attempts
 *                                    y si se agotan: basicNack(requeue=false) => DLQ
 *
 * Todo mensaje que va a la DLQ queda registrado en el log con su causa y su cuerpo. La DLQ se
 * deja intacta (nadie la consume) para poder inspeccionar y reprocesar los mensajes muertos.
 */
@Component
public class AckSupport {

    private static final Logger log = LoggerFactory.getLogger(AckSupport.class);
    private static final int MAX_BODY_LOG = 500;

    private final MessagingProperties props;

    public AckSupport(MessagingProperties props) {
        this.props = props;
    }

    public void handle(String listenerId, String deadLetterQueue, Message message,
                       Channel channel, ThrowingRunnable work) {
        long tag = message.getMessageProperties().getDeliveryTag();
        int maxAttempts = Math.max(1, props.retry().maxAttempts());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                work.run();
                ack(listenerId, channel, tag);
                return;
            } catch (NonRetryableMessageException e) {
                reject(listenerId, deadLetterQueue, message, channel, tag,
                        "error no recuperable: " + e.getMessage());
                return;
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    reject(listenerId, deadLetterQueue, message, channel, tag,
                            "reintentos agotados (" + maxAttempts + "): " + e);
                    return;
                }
                log.warn("[{}] intento {}/{} fallo, se reintenta en {} ms: {}",
                        listenerId, attempt, maxAttempts, props.retry().backoffMs(), e.toString());
                if (!esperar(props.retry().backoffMs())) {
                    requeue(listenerId, channel, tag);
                    return;
                }
            }
        }
    }

    private void ack(String listenerId, Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            // El broker volvera a entregar el mensaje; los consumidores son idempotentes.
            log.error("[{}] no se pudo confirmar (ACK) el mensaje tag={}: {}", listenerId, tag, e.toString());
        }
    }

    private void reject(String listenerId, String deadLetterQueue, Message message,
                        Channel channel, long tag, String causa) {
        log.error("[{}] mensaje enviado a la DLQ '{}' messageId={} causa={} cuerpo={}",
                listenerId, deadLetterQueue, message.getMessageProperties().getMessageId(), causa, cuerpo(message));
        try {
            channel.basicNack(tag, false, false);
        } catch (IOException e) {
            log.error("[{}] no se pudo rechazar (NACK) el mensaje tag={}: {}", listenerId, tag, e.toString());
        }
    }

    private void requeue(String listenerId, Channel channel, long tag) {
        log.warn("[{}] consumidor interrumpido, el mensaje tag={} vuelve a la cola", listenerId, tag);
        try {
            channel.basicNack(tag, false, true);
        } catch (IOException e) {
            log.error("[{}] no se pudo devolver el mensaje tag={} a la cola: {}", listenerId, tag, e.toString());
        }
    }

    private boolean esperar(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String cuerpo(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        return body.length() <= MAX_BODY_LOG ? body : body.substring(0, MAX_BODY_LOG) + "...";
    }
}

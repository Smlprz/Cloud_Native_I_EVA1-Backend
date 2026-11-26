package com.petshop.pago.messaging.rabbit;

import com.petshop.pago.domain.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica pago.procesado (RabbitMQ, punto a punto) para que ms-orden marque la orden PAGADA o PAGO_RECHAZADO.
 * Si RabbitMQ falla, el pago ya guardado no se pierde: se registra el error.
 */
@Component
public class PagoProcesadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoProcesadoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public PagoProcesadoPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicar(Pago pago) {
        PagoProcesadoEvent evento = new PagoProcesadoEvent(pago.getId(), pago.getOrdenId(), pago.getEstado().name());
        try {
            rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().pagoProcesado(), evento);
            log.info("Publicado '{}' de la orden {} ({})", props.routingKeys().pagoProcesado(), pago.getOrdenId(), evento.estado());
        } catch (Exception e) {
            log.error("No se pudo publicar '{}' de la orden {}: {}", props.routingKeys().pagoProcesado(),
                    pago.getOrdenId(), e.getMessage());
        }
    }

    /** estado = APROBADO | RECHAZADO | BLOQUEADO_FRAUDE */
    public record PagoProcesadoEvent(Long pagoId, Long ordenId, String estado) {
    }
}

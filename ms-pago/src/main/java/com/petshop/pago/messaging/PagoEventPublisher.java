package com.petshop.pago.messaging;

import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Publica el resultado de cada pago en el topico eventos.pagos. El pago ya esta guardado en la base de
 * datos: si Kafka no responde solo se deja el error en el log y el pago NO se deshace.
 */
@Component
public class PagoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoEventPublisher.class);

    private final KafkaTemplate<String, PagoEvent> kafka;
    private final String topico;

    public PagoEventPublisher(KafkaTemplate<String, PagoEvent> kafka,
                              @Value("${kafka.topics.pagos}") String topico) {
        this.kafka = kafka;
        this.topico = topico;
    }

    public void publicar(Pago pago) {
        PagoEvent evento = new PagoEvent(
                UUID.randomUUID().toString(),
                tipoDe(pago.getEstado()),
                pago.getOrdenId(),
                pago.getUsuarioId(),
                pago.getMonto(),
                pago.getTarjetaUltimos4(),
                pago.getMotivo(),
                pago.getCreadoEn().atZone(ZoneId.systemDefault()).toInstant());
        try {
            // La clave es el usuario: sus eventos caen en la misma particion y se leen en orden.
            kafka.send(topico, pago.getUsuarioId(), evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar {} de la orden {} en {}: {}",
                            evento.tipo(), evento.ordenId(), topico, error.getMessage());
                } else {
                    log.debug("Evento {} publicado en {} (particion {}, offset {})", evento.tipo(), topico,
                            resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Kafka no disponible: no se publico {} de la orden {}: {}",
                    evento.tipo(), evento.ordenId(), e.getMessage());
        }
    }

    static String tipoDe(EstadoPago estado) {
        return switch (estado) {
            case APROBADO -> PagoEvent.PAGO_APROBADO;
            case RECHAZADO -> PagoEvent.PAGO_RECHAZADO;
            case BLOQUEADO_FRAUDE -> PagoEvent.FRAUDE_BLOQUEADO;
        };
    }
}

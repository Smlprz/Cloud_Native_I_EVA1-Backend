package com.petshop.cupones.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Publica CUPON_APLICADO en eventos.navegacion. Es "best effort": el cupon ya se aplico en la base de datos, asi que
 * si Kafka no responde solo se deja el aviso en el log y la compra no se afecta.
 */
@Component
public class CuponEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CuponEventPublisher.class);

    private final KafkaTemplate<String, NavegacionEvent> kafka;
    private final String topico;
    private final boolean habilitado;

    public CuponEventPublisher(KafkaTemplate<String, NavegacionEvent> kafka,
                               @Value("${kafka.topics.navegacion}") String topico,
                               @Value("${kafka.publish-enabled:true}") boolean habilitado) {
        this.kafka = kafka;
        this.topico = topico;
        this.habilitado = habilitado;
    }

    public void cuponAplicado(String usuarioId, String codigo, BigDecimal descuento) {
        if (!habilitado) {
            return;
        }
        NavegacionEvent evento = new NavegacionEvent(UUID.randomUUID().toString(), NavegacionEvent.CUPON_APLICADO,
                usuarioId, null, null, null, descuento, null, "ms-cupones", Instant.now(),
                "cupon " + codigo + ": -$" + descuento.toPlainString());
        try {
            // La clave es el usuario: sus eventos caen en la misma particion y se leen en orden.
            kafka.send(topico, usuarioId, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.warn("No se pudo publicar CUPON_APLICADO ({}) en {}: {}", codigo, topico, error.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka no disponible: no se publico CUPON_APLICADO ({}): {}", codigo, e.getMessage());
        }
    }
}

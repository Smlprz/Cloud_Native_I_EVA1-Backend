package com.petshop.productos.messaging.kafka;

import com.petshop.productos.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica los eventos de navegacion en eventos.navegacion. Es "best effort": la API del catalogo nunca debe
 * fallar ni demorarse porque Kafka no responda, asi que cualquier error solo queda en el log.
 */
@Component
public class NavegacionPublisher {

    private static final Logger log = LoggerFactory.getLogger(NavegacionPublisher.class);
    private static final String ORIGEN = "ms-productos";

    private final KafkaTemplate<String, NavegacionEvent> kafka;
    private final String topico;
    private final boolean habilitado;

    public NavegacionPublisher(KafkaTemplate<String, NavegacionEvent> kafka,
                               @Value("${kafka.topics.navegacion}") String topico,
                               @Value("${kafka.publish-enabled:true}") boolean habilitado) {
        this.kafka = kafka;
        this.topico = topico;
        this.habilitado = habilitado;
    }

    /** El usuario abrio el detalle de un producto. usuarioId es "anonimo" si no hay sesion. */
    public void productoVisto(String usuarioId, Product producto) {
        if (!habilitado) {
            return;
        }
        NavegacionEvent evento = new NavegacionEvent(
                UUID.randomUUID().toString(),
                NavegacionEvent.PRODUCTO_VISTO,
                usuarioId,
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria(),
                producto.getPrecio(),
                null,
                ORIGEN,
                Instant.now());
        try {
            // La clave es el usuario: sus eventos caen en la misma particion y se leen en orden.
            kafka.send(topico, usuarioId, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.warn("No se pudo publicar {} del producto {} en {}: {}",
                            evento.tipo(), evento.productoId(), topico, error.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka no disponible: no se publico {} del producto {}: {}",
                    evento.tipo(), evento.productoId(), e.getMessage());
        }
    }
}

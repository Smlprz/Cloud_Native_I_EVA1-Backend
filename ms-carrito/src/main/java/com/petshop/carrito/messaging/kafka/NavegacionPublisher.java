package com.petshop.carrito.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Publica los eventos de navegacion del carrito en eventos.navegacion. Es "best effort": el carrito nunca debe
 * fallar ni demorarse porque Kafka no responda, asi que cualquier error solo queda en el log.
 */
@Component
public class NavegacionPublisher {

    private static final Logger log = LoggerFactory.getLogger(NavegacionPublisher.class);
    private static final String ORIGEN = "ms-carrito";

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

    public void itemAgregado(String usuarioId, Long productoId, String nombre, String categoria,
                             BigDecimal precio, int cantidad) {
        publicar(NavegacionEvent.ITEM_AGREGADO, usuarioId, productoId, nombre, categoria, precio, cantidad);
    }

    public void itemEliminado(String usuarioId, Long productoId, String nombre, BigDecimal precio, int cantidad) {
        publicar(NavegacionEvent.ITEM_ELIMINADO, usuarioId, productoId, nombre, null, precio, cantidad);
    }

    /** Un evento por cada producto del pedido confirmado. */
    public void compraRealizada(String usuarioId, Long productoId, String nombre, BigDecimal precio, int cantidad) {
        publicar(NavegacionEvent.COMPRA_REALIZADA, usuarioId, productoId, nombre, null, precio, cantidad);
    }

    /** Envio (despacho) del pedido confirmado: nombre = "Pedido N° x", cantidad = unidades, precio = total. */
    public void envioGestionado(String usuarioId, Long pedidoId, BigDecimal total, int unidades) {
        publicar(NavegacionEvent.ENVIO_GESTIONADO, usuarioId, null, "Pedido N° " + pedidoId, null, total, unidades);
    }

    private void publicar(String tipo, String usuarioId, Long productoId, String nombre, String categoria,
                          BigDecimal precio, int cantidad) {
        if (!habilitado) {
            return;
        }
        NavegacionEvent evento = new NavegacionEvent(UUID.randomUUID().toString(), tipo, usuarioId, productoId,
                nombre, categoria, precio, cantidad, ORIGEN, Instant.now());
        try {
            // La clave es el usuario: sus eventos caen en la misma particion y se leen en orden.
            kafka.send(topico, usuarioId, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.warn("No se pudo publicar {} del producto {} en {}: {}",
                            tipo, productoId, topico, error.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka no disponible: no se publico {} del producto {}: {}", tipo, productoId, e.getMessage());
        }
    }
}

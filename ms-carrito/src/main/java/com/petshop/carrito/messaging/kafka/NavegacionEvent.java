package com.petshop.carrito.messaging.kafka;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de navegacion publicado en el topico eventos.navegacion (clickstream). El mismo contrato lo
 * publican ms-productos y ms-carrito y lo consume ms-analytics-recommendations.
 *
 * eventId         identificador unico (permite detectar duplicados)
 * tipo            PRODUCTO_VISTO | ITEM_AGREGADO | ITEM_ELIMINADO | COMPRA_REALIZADA | CUPON_APLICADO | ENVIO_GESTIONADO
 * usuarioId       usuario (clave del mensaje: sus eventos van a la misma particion y se leen en orden)
 * productoId      producto implicado (null en eventos que no se refieren a uno)
 * nombreProducto  nombre del producto, si el publicador lo conoce
 * categoria       categoria del producto, si el publicador la conoce (alimenta las recomendaciones)
 * precio          precio unitario, si se conoce
 * cantidad        unidades (agregar, quitar, comprar)
 * origen          microservicio que publica (ms-productos / ms-carrito)
 * timestamp       momento de la interaccion (ISO-8601)
 */
public record NavegacionEvent(
        String eventId,
        String tipo,
        String usuarioId,
        Long productoId,
        String nombreProducto,
        String categoria,
        BigDecimal precio,
        Integer cantidad,
        String origen,
        Instant timestamp
) {
    public static final String ITEM_AGREGADO = "ITEM_AGREGADO";
    public static final String ITEM_ELIMINADO = "ITEM_ELIMINADO";
    public static final String COMPRA_REALIZADA = "COMPRA_REALIZADA";
}

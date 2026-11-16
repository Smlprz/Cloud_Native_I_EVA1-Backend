package com.petshop.analytics.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de navegacion del topico eventos.navegacion (clickstream). Lo publican ms-productos (PRODUCTO_VISTO)
 * y ms-carrito (ITEM_AGREGADO, ITEM_ELIMINADO, COMPRA_REALIZADA). Es el MISMO contrato que publican ellos:
 * si el productor agrega un campo nuevo, este consumidor lo ignora y sigue funcionando.
 *
 * eventId         identificador unico (permite descartar duplicados)
 * tipo            tipo de interaccion (ver constantes)
 * usuarioId       usuario; es la clave del mensaje, asi sus eventos llegan en orden a la misma particion
 * productoId      producto implicado (null si el evento no se refiere a uno)
 * nombreProducto  nombre del producto, si el publicador lo conocia
 * categoria       categoria del producto, si el publicador la conocia
 * precio          precio unitario, si se conocia
 * cantidad        unidades (agregar, quitar, comprar)
 * origen          microservicio que publico el evento
 * timestamp       momento de la interaccion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    public static final String PRODUCTO_VISTO = "PRODUCTO_VISTO";
    public static final String ITEM_AGREGADO = "ITEM_AGREGADO";
    public static final String ITEM_ELIMINADO = "ITEM_ELIMINADO";
    public static final String COMPRA_REALIZADA = "COMPRA_REALIZADA";
    public static final String CUPON_APLICADO = "CUPON_APLICADO";
    public static final String ENVIO_GESTIONADO = "ENVIO_GESTIONADO";
}

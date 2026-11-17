package com.petshop.cupones.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de navegacion del topico eventos.navegacion: el MISMO contrato que publican ms-productos y ms-carrito y
 * que consume ms-analytics-recommendations, mas el campo opcional {@code detalle} (los consumidores ignoran los
 * campos que no conocen). En un CUPON_APLICADO: precio = monto descontado y detalle = codigo y descuento.
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
        Instant timestamp,
        String detalle
) {
    public static final String CUPON_APLICADO = "CUPON_APLICADO";
}

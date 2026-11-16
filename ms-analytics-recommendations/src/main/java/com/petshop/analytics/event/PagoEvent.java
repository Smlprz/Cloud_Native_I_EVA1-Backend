package com.petshop.analytics.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento del topico eventos.pagos que publica ms-pago.
 *
 * eventId         identificador unico
 * tipo            PAGO_APROBADO | PAGO_RECHAZADO | FRAUDE_BLOQUEADO
 * ordenId         orden a la que corresponde el pago
 * usuarioId       usuario (clave del mensaje)
 * monto           monto de la orden
 * tarjetaUltimos4 ultimos 4 digitos de la tarjeta (nunca el numero completo)
 * motivo          por que se rechazo o bloqueo (null si fue aprobado)
 * timestamp       momento del pago
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoEvent(
        String eventId,
        String tipo,
        Long ordenId,
        String usuarioId,
        BigDecimal monto,
        String tarjetaUltimos4,
        String motivo,
        Instant timestamp
) {
    public static final String PAGO_APROBADO = "PAGO_APROBADO";
    public static final String PAGO_RECHAZADO = "PAGO_RECHAZADO";
    public static final String FRAUDE_BLOQUEADO = "FRAUDE_BLOQUEADO";
}

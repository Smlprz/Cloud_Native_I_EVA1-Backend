package com.petshop.pago.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento publicado en el topico eventos.pagos. El mismo contrato lo consume ms-analytics-recommendations.
 *
 * eventId   identificador unico (permite detectar duplicados)
 * tipo      PAGO_APROBADO | PAGO_RECHAZADO | FRAUDE_BLOQUEADO
 * ordenId   orden pagada (clave de negocio)
 * usuarioId usuario (clave del mensaje: todos los eventos de un usuario van a la misma particion, en orden)
 * monto     monto de la orden
 * tarjetaUltimos4  ultimos 4 digitos (nunca el numero completo)
 * motivo    por que se rechazo o bloqueo (null si fue aprobado)
 * timestamp momento del pago (ISO-8601)
 */
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

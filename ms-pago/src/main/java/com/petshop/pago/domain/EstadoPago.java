package com.petshop.pago.domain;

/** Resultado de un intento de pago. */
public enum EstadoPago {
    APROBADO,
    RECHAZADO,
    BLOQUEADO_FRAUDE
}

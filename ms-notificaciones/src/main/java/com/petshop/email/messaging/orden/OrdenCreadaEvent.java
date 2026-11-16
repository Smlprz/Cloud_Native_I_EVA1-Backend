package com.petshop.email.messaging.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Mensaje que publica ms-orden cuando se crea una orden. Se valida al recibirlo: sin email de
 * destino no hay a quien notificar y el mensaje va a la DLQ sin reintentos.
 */
public record OrdenCreadaEvent(
        @NotNull Long ordenId,
        @NotBlank String email,
        @NotNull BigDecimal total) {
}

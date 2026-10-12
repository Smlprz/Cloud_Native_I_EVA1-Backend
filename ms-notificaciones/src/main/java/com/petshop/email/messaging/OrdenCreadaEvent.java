package com.petshop.email.messaging;

import java.math.BigDecimal;

/** Mensaje que publica ms-orden cuando se crea una orden. */
public record OrdenCreadaEvent(Long ordenId, String email, BigDecimal total) {
}

package com.petshop.orden.messaging;

import java.math.BigDecimal;

/** Mensaje que se publica al crear una orden. ms-notificaciones lo consume. */
public record OrdenCreadaEvent(Long ordenId, String email, BigDecimal total) {
}

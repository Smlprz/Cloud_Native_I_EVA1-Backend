package com.petshop.pago.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Vista parcial de una orden tal como la devuelve ms-orden. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenDto(Long id, BigDecimal total, String estado) {
}

package com.pedidos360.carrito.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Vista parcial del producto tal como lo devuelve ms-productos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(
        Long id,
        String nombre,
        BigDecimal precio,
        int stock
) {
}

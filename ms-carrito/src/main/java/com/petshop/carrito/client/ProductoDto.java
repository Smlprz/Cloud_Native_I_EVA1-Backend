package com.petshop.carrito.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Vista parcial del producto tal como lo devuelve ms-productos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(
        Long id,
        String nombre,
        BigDecimal precio,
        int stock,
        String categoria
) {

    /** Constructor sin categoria (compatible con el codigo y las pruebas anteriores). */
    public ProductoDto(Long id, String nombre, BigDecimal precio, int stock) {
        this(id, nombre, precio, stock, null);
    }
}

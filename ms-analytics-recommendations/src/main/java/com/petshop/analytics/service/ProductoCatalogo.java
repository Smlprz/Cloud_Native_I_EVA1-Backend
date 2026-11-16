package com.petshop.analytics.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Producto del catalogo tal como lo publica ms-productos (GET /api/productos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoCatalogo(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int stock,
        String categoria,
        String imagenUrl
) {
}

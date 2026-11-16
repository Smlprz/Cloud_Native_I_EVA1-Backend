package com.petshop.analytics.service;

import java.math.BigDecimal;

/** Producto sugerido a un usuario, con el motivo y el puntaje con que se eligio. */
public record Recomendacion(
        Long productoId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String categoria,
        String imagenUrl,
        String motivo,
        double puntaje
) {
}

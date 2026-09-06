package com.pedidos360.productos.web.dto;

import com.pedidos360.productos.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int stock,
        String categoria,
        String imagenUrl,
        Long idVendedor
) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getCategoria(),
                p.getImagenUrl(),
                p.getIdVendedor()
        );
    }
}

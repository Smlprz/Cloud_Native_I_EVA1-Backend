package com.pedidos360.carrito.web.dto;

import com.pedidos360.carrito.domain.Carrito;

import java.math.BigDecimal;
import java.util.List;

public record CarritoResponse(
        Long id,
        String estado,
        List<ItemResponse> items,
        BigDecimal total
) {

    public record ItemResponse(
            Long id,
            Long productoId,
            String nombreProducto,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal
    ) {
    }

    public static CarritoResponse from(Carrito carrito) {
        List<ItemResponse> items = carrito.getItems().stream()
                .map(i -> new ItemResponse(
                        i.getId(),
                        i.getProductoId(),
                        i.getNombreProducto(),
                        i.getPrecioUnitario(),
                        i.getCantidad(),
                        i.getSubtotal()))
                .toList();
        return new CarritoResponse(carrito.getId(), carrito.getEstado(), items, carrito.getTotal());
    }
}

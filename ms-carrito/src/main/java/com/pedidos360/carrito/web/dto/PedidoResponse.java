package com.pedidos360.carrito.web.dto;

import com.pedidos360.carrito.domain.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        BigDecimal total,
        String estado,
        LocalDateTime creadoEn,
        List<ItemResponse> items
) {

    public record ItemResponse(
            Long productoId,
            String nombreProducto,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal
    ) {
    }

    public static PedidoResponse from(Pedido pedido) {
        List<ItemResponse> items = pedido.getItems().stream()
                .map(i -> new ItemResponse(
                        i.getProductoId(),
                        i.getNombreProducto(),
                        i.getPrecioUnitario(),
                        i.getCantidad(),
                        i.getSubtotal()))
                .toList();
        return new PedidoResponse(pedido.getId(), pedido.getTotal(), pedido.getEstado(),
                pedido.getCreadoEn(), items);
    }
}

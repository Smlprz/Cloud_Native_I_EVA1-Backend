package com.petshop.orden.messaging;

import java.util.List;

/**
 * Comando stock.descontar: ms-orden le pide a ms-productos descontar el stock de una orden.
 * ordenId identifica la orden y hace idempotente el descuento (un duplicado no descuenta dos veces).
 */
public record StockDescontarCommand(Long ordenId, List<Linea> items) {

    public record Linea(Long productoId, int cantidad) {
    }
}

package com.petshop.productos.messaging.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Comando stock.descontar: lo publica ms-orden al crear una orden con items.
 * Se valida al recibirlo; un mensaje invalido va directo a la DLQ (no se reintenta).
 */
public record StockDescontarCommand(
        @NotNull Long ordenId,
        @NotEmpty List<@Valid Linea> items) {

    public record Linea(@NotNull Long productoId, @Positive int cantidad) {
    }
}

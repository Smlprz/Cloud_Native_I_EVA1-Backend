package com.petshop.email.messaging.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Evento stock.bajo (exchange topic): lo publica ms-productos cuando un producto queda con poco stock. */
public record StockBajoEvent(
        @NotNull Long productoId,
        @NotBlank String nombre,
        @PositiveOrZero int stock,
        @PositiveOrZero int umbral) {
}

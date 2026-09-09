package com.pedidos360.carrito.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarItemRequest(

        @NotNull(message = "cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad minima es 1")
        Integer cantidad
) {
}

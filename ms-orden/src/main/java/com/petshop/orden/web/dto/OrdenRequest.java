package com.petshop.orden.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * email: opcional. Si viene, el comprobante se envia ahi; si no, al correo del token.
 * items: opcional. Si vienen, ms-orden le pide a ms-productos descontar el stock por RabbitMQ.
 */
public record OrdenRequest(
        @NotNull @DecimalMin("0.01") BigDecimal total,
        @Email @Size(max = 254) String email,
        @Valid @Size(max = 200) List<ItemRequest> items
) {

    public record ItemRequest(
            @NotNull Long productoId,
            @Positive int cantidad
    ) {
    }
}

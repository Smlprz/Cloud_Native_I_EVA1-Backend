package com.petshop.orden.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** email: opcional. Si viene, el comprobante se envia ahi; si no, al correo del token. */
public record OrdenRequest(
        @NotNull @DecimalMin("0.01") BigDecimal total,
        @Email @Size(max = 254) String email
) {
}

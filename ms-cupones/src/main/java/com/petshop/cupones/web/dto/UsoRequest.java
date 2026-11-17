package com.petshop.cupones.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Cupon a validar o aplicar y el monto de la compra sobre el que se calcula el descuento. */
public record UsoRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$", message = "codigo de cupon invalido")
        String codigo,

        @NotNull @DecimalMin(value = "0.01", message = "el monto debe ser mayor que 0") @Digits(integer = 10, fraction = 2)
        BigDecimal monto
) {
}

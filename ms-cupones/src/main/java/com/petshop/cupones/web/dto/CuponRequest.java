package com.petshop.cupones.web.dto;

import com.petshop.cupones.domain.TipoCupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos para crear un cupon. valor: porcentaje (1 a 100) o pesos, segun el tipo.
 * montoMinimo, venceEn y usosMaximos son opcionales.
 */
public record CuponRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$", message = "el codigo debe tener 3 a 40 caracteres: letras, numeros, _ y -")
        String codigo,

        @NotNull
        TipoCupon tipo,

        @NotNull @DecimalMin(value = "0.01", message = "el valor debe ser mayor que 0") @Digits(integer = 10, fraction = 2)
        BigDecimal valor,

        @DecimalMin(value = "0", message = "el monto minimo no puede ser negativo") @Digits(integer = 10, fraction = 2)
        BigDecimal montoMinimo,

        LocalDateTime venceEn,

        @Min(value = 1, message = "usosMaximos debe ser al menos 1")
        Integer usosMaximos
) {
}

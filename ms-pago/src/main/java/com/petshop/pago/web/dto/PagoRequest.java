package com.petshop.pago.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para pagar una orden. El monto NO viene aqui: se toma de la orden.
 * El numero de tarjeta y el CVV se usan solo para validar el pago y no se guardan.
 */
public record PagoRequest(
        @NotNull @Positive
        Long ordenId,

        @NotBlank
        @Pattern(regexp = "^[0-9][0-9 -]{11,21}[0-9]$", message = "numero de tarjeta invalido (12 a 19 digitos)")
        String numeroTarjeta,

        @NotBlank @Size(max = 100)
        String titular,

        @NotBlank
        @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "el vencimiento debe tener el formato MM/AA")
        String vencimiento,

        @NotBlank
        @Pattern(regexp = "^[0-9]{3,4}$", message = "cvv invalido (3 o 4 digitos)")
        String cvv
) {

    /** Nunca imprime los datos de la tarjeta (por si el objeto termina en un log). */
    @Override
    public String toString() {
        return "PagoRequest[ordenId=" + ordenId + ", tarjeta=****]";
    }
}

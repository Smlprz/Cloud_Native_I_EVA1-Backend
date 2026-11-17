package com.petshop.carrito.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo opcional del checkout.
 *  email: direccion a la que se envia el comprobante.
 *  cupon: codigo de un cupon de descuento (ms-cupones); si es valido, el total del pedido baja.
 */
public record CheckoutRequest(
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$", message = "codigo de cupon invalido") String cupon
) {

    public CheckoutRequest(String email) {
        this(email, null);
    }
}

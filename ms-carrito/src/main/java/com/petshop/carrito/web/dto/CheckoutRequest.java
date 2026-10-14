package com.petshop.carrito.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Cuerpo opcional del checkout. email: direccion a la que se envia el comprobante. */
public record CheckoutRequest(@Email @Size(max = 254) String email) {
}

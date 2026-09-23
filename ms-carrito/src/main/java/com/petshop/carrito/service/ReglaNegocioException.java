package com.petshop.carrito.service;

/** Error de regla de negocio -> se traduce a HTTP 400. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String message) {
        super(message);
    }
}

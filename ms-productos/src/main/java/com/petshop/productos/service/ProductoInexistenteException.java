package com.petshop.productos.service;

/** La resena apunta a un producto que no existe: error de negocio, no se reintenta. */
public class ProductoInexistenteException extends RuntimeException {

    public ProductoInexistenteException(String message) {
        super(message);
    }
}

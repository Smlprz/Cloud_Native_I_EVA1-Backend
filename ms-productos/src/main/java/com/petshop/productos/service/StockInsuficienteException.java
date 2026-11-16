package com.petshop.productos.service;

/** El producto no existe o no tiene stock suficiente para la cantidad pedida. */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String message) {
        super(message);
    }
}

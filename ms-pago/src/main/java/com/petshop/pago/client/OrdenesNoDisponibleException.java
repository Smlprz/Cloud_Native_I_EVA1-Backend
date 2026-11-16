package com.petshop.pago.client;

/** ms-orden no respondio: no se puede saber el monto ni el dueno de la orden. */
public class OrdenesNoDisponibleException extends RuntimeException {

    public OrdenesNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

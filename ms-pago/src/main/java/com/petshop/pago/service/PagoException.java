package com.petshop.pago.service;

import org.springframework.http.HttpStatus;

/** Error de negocio del pago con el codigo HTTP que le corresponde. */
public class PagoException extends RuntimeException {

    private final HttpStatus status;

    private PagoException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public static PagoException ordenNoEncontrada(Long ordenId) {
        return new PagoException(HttpStatus.NOT_FOUND, "La orden " + ordenId + " no existe o no es tuya");
    }

    public static PagoException ordenYaPagada(Long ordenId) {
        return new PagoException(HttpStatus.CONFLICT, "La orden " + ordenId + " ya fue pagada");
    }

    public HttpStatus getStatus() {
        return status;
    }
}

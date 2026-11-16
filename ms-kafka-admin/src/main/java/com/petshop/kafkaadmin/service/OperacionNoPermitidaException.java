package com.petshop.kafkaadmin.service;

/** La operacion es valida pero no esta permitida (por ejemplo, eliminar un topico del sistema). */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}

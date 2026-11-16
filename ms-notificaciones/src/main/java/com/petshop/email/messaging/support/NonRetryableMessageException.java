package com.petshop.email.messaging.support;

/**
 * Error que no se arregla reintentando (JSON roto, evento invalido, regla de negocio violada).
 * El consumidor lo envia a la DLQ de inmediato, sin gastar reintentos.
 */
public class NonRetryableMessageException extends RuntimeException {

    public NonRetryableMessageException(String message) {
        super(message);
    }

    public NonRetryableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

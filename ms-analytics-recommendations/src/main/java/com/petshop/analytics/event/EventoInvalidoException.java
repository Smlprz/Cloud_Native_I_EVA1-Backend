package com.petshop.analytics.event;

/**
 * El mensaje llego bien pero su contenido no es valido (falta un dato, tipo desconocido...). No tiene sentido
 * reintentarlo: se registra en el log con su contenido y se continua con el siguiente.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}

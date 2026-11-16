package com.petshop.rabbitadmin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/** Errores de la API de administracion con un mensaje claro (RFC 7807, ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Cuerpo JSON invalido: lista cada campo con su problema. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getAllErrors().stream()
                .map(e -> (e instanceof org.springframework.validation.FieldError f ? f.getField() : e.getObjectName())
                        + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    /** Parametro de ruta invalido (por ejemplo DELETE /queues/amq.algo). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametroInvalido(HandlerMethodValidationException ex) {
        String detalle = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    /** No hay conexion con el broker. */
    @ExceptionHandler(AmqpConnectException.class)
    public ProblemDetail brokerNoDisponible(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "RabbitMQ no disponible: " + ex.getMessage());
    }

    /** El broker respondio pero rechazo la operacion (p. ej. redeclarar una cola con otros argumentos). */
    @ExceptionHandler(AmqpException.class)
    public ProblemDetail operacionRechazada(AmqpException ex) {
        log.warn("RabbitMQ rechazo la operacion: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "RabbitMQ rechazo la operacion: " + causaRaiz(ex));
    }

    private static String causaRaiz(Throwable t) {
        Throwable actual = t;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return String.valueOf(actual.getMessage());
    }
}

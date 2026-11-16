package com.petshop.pago.web;

import com.petshop.pago.client.OrdenesNoDisponibleException;
import com.petshop.pago.service.PagoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Errores de la API de pagos con un mensaje claro (RFC 7807, ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Cuerpo invalido. Lista cada campo con su problema, sin devolver nunca el valor enviado. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getAllErrors().stream()
                .map(e -> (e instanceof org.springframework.validation.FieldError f ? f.getField() : e.getObjectName())
                        + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(PagoException.class)
    public ProblemDetail errorDeNegocio(PagoException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(OrdenesNoDisponibleException.class)
    public ProblemDetail ordenesNoDisponible(OrdenesNoDisponibleException ex) {
        log.error(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "No se pudo verificar la orden en este momento, intenta nuevamente");
    }
}

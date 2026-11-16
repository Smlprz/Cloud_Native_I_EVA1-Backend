package com.petshop.analytics.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/** Errores de la API con un mensaje claro (RFC 7807, ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Parametro invalido (por ejemplo limite=500 o productoId=-1). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametroInvalido(HandlerMethodValidationException ex) {
        String detalle = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }
}

package com.petshop.cupones.web;

import com.petshop.cupones.service.CuponException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Errores de la API con un mensaje claro (RFC 7807, ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CuponException.class)
    public ProblemDetail cuponNoUtilizable(CuponException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getAllErrors().stream()
                .map(e -> (e instanceof org.springframework.validation.FieldError f ? f.getField() : e.getObjectName())
                        + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }
}

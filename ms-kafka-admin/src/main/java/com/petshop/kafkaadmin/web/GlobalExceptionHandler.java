package com.petshop.kafkaadmin.web;

import com.petshop.kafkaadmin.service.OperacionNoPermitidaException;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.DisconnectException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** Parametro de ruta invalido (por ejemplo DELETE /topics/nombre%20raro). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametroInvalido(HandlerMethodValidationException ex) {
        String detalle = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ProblemDetail noPermitida(OperacionNoPermitidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({UnknownTopicOrPartitionException.class, GroupIdNotFoundException.class})
    public ProblemDetail noEncontrado(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No existe: " + ex.getMessage());
    }

    @ExceptionHandler({TopicExistsException.class, GroupNotEmptyException.class})
    public ProblemDetail conflicto(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidTopicException.class, InvalidConfigurationException.class, InvalidPartitionsException.class,
            InvalidReplicationFactorException.class, InvalidRequestException.class})
    public ProblemDetail peticionInvalida(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** El cluster no responde a tiempo. */
    @ExceptionHandler({TimeoutException.class, DisconnectException.class})
    public ProblemDetail clusterNoDisponible(ApiException ex) {
        log.error("Kafka no disponible: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no disponible: " + ex.getMessage());
    }

    /** Cualquier otro rechazo del cluster. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail operacionRechazada(ApiException ex) {
        log.warn("Kafka rechazo la operacion: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Kafka rechazo la operacion: " + ex.getMessage());
    }
}

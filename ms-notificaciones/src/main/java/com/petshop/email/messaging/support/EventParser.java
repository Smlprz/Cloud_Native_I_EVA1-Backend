package com.petshop.email.messaging.support;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte el cuerpo del mensaje al evento esperado y lo valida. Cualquier fallo es
 * {@link NonRetryableMessageException}: un mensaje malformado no mejora al reintentarlo.
 * (Este servicio usa Spring Boot 4 / Jackson 3, cuyas excepciones de JSON no son checked.)
 */
@Component
public class EventParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EventParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> T parse(Message message, Class<T> type) {
        T event;
        try {
            event = objectMapper.readValue(message.getBody(), type);
        } catch (RuntimeException e) {
            String primeraLinea = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
            throw new NonRetryableMessageException(
                    "JSON invalido para " + type.getSimpleName() + ": " + primeraLinea, e);
        }
        if (event == null) {
            throw new NonRetryableMessageException("Cuerpo vacio para " + type.getSimpleName());
        }
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detalle = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new NonRetryableMessageException(type.getSimpleName() + " invalido: " + detalle);
        }
        return event;
    }
}

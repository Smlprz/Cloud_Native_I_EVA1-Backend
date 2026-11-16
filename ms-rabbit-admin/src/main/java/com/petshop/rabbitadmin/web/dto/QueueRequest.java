package com.petshop.rabbitadmin.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Si se indica deadLetterExchange, los mensajes rechazados o vencidos se envian ahi (DLQ).
 * deadLetterRoutingKey solo tiene sentido junto con deadLetterExchange.
 */
public record QueueRequest(
        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Size(max = NombresRabbit.MAX, message = "el nombre no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.NOMBRE_REGEX, message = NombresRabbit.NOMBRE_MENSAJE)
        String name,

        Boolean durable,

        @Size(max = NombresRabbit.MAX, message = "el exchange de mensajes muertos no puede superar 255 caracteres")
        @Pattern(regexp = "^$|" + NombresRabbit.NOMBRE_REGEX, message = NombresRabbit.NOMBRE_MENSAJE)
        String deadLetterExchange,

        @Size(max = NombresRabbit.MAX, message = "la routing key no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.ROUTING_KEY_REGEX, message = NombresRabbit.ROUTING_KEY_MENSAJE)
        String deadLetterRoutingKey
) {

    @JsonIgnore
    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterCoherente() {
        boolean hayRoutingKey = deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank();
        boolean hayExchange = deadLetterExchange != null && !deadLetterExchange.isBlank();
        return !hayRoutingKey || hayExchange;
    }
}

package com.petshop.rabbitadmin.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Si se indica deadLetterExchange, los mensajes rechazados o vencidos se envian ahi (DLQ). */
public record QueueRequest(
        @NotBlank String name,
        Boolean durable,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}

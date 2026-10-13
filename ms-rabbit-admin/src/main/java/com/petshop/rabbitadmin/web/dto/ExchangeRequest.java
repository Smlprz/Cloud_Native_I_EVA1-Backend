package com.petshop.rabbitadmin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** type: direct, topic, fanout o headers (por defecto direct). */
public record ExchangeRequest(
        @NotBlank String name,
        @Pattern(regexp = "direct|topic|fanout|headers", message = "type debe ser direct, topic, fanout o headers")
        String type,
        Boolean durable
) {
}

package com.petshop.rabbitadmin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** type: direct, topic, fanout o headers (por defecto direct). */
public record ExchangeRequest(
        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Size(max = NombresRabbit.MAX, message = "el nombre no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.NOMBRE_REGEX, message = NombresRabbit.NOMBRE_MENSAJE)
        String name,

        @Pattern(regexp = "direct|topic|fanout|headers", message = "type debe ser direct, topic, fanout o headers")
        String type,

        Boolean durable
) {
}

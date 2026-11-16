package com.petshop.rabbitadmin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BindingRequest(
        @NotBlank(message = "la cola es obligatoria")
        @Size(max = NombresRabbit.MAX, message = "el nombre de la cola no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.NOMBRE_REGEX, message = NombresRabbit.NOMBRE_MENSAJE)
        String queue,

        @NotBlank(message = "el exchange es obligatorio")
        @Size(max = NombresRabbit.MAX, message = "el nombre del exchange no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.NOMBRE_REGEX, message = NombresRabbit.NOMBRE_MENSAJE)
        String exchange,

        @Size(max = NombresRabbit.MAX, message = "la routing key no puede superar 255 caracteres")
        @Pattern(regexp = NombresRabbit.ROUTING_KEY_REGEX, message = NombresRabbit.ROUTING_KEY_MENSAJE)
        String routingKey
) {
}

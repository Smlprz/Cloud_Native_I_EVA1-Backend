package com.petshop.rabbitadmin.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BindingRequest(
        @NotBlank String queue,
        @NotBlank String exchange,
        String routingKey
) {
}

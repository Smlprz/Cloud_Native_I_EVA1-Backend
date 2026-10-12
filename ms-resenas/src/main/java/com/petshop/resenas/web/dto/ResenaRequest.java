package com.petshop.resenas.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResenaRequest(
        @NotNull Long productoId,
        @Min(1) @Max(5) int puntaje,
        @Size(max = 1000) String comentario
) {
}

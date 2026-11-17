package com.petshop.cupones.web.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/cupones/{id}/activo. */
public record ActivoRequest(@NotNull Boolean activo) {
}

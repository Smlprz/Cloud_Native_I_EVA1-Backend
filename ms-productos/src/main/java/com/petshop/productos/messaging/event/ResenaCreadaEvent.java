package com.petshop.productos.messaging.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Evento resena.creada: lo publica ms-resenas al guardar una resena.
 * Se valida al recibirlo; un mensaje invalido va directo a la DLQ (no se reintenta).
 */
public record ResenaCreadaEvent(
        @NotNull Long resenaId,
        @NotNull Long productoId,
        @Min(1) @Max(5) int puntaje) {
}

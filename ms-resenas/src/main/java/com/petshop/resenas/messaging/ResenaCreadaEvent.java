package com.petshop.resenas.messaging;

/** Evento resena.creada: lo consume ms-productos para actualizar la calificacion del producto. */
public record ResenaCreadaEvent(Long resenaId, Long productoId, int puntaje) {
}

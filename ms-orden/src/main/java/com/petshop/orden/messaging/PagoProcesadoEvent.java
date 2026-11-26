package com.petshop.orden.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Evento pago.procesado: lo publica ms-pago. estado = APROBADO | RECHAZADO | BLOQUEADO_FRAUDE. */
public record PagoProcesadoEvent(Long pagoId, @NotNull Long ordenId, @NotBlank String estado) {
}

package com.petshop.pago.web.dto;

import com.petshop.pago.domain.AlertaFraude;

import java.time.LocalDateTime;

public record AlertaFraudeResponse(
        Long id,
        String usuarioId,
        Long ordenId,
        String tarjetaUltimos4,
        int intentos,
        int ventanaSegundos,
        LocalDateTime creadoEn
) {

    public static AlertaFraudeResponse from(AlertaFraude a) {
        return new AlertaFraudeResponse(a.getId(), a.getUsuarioId(), a.getOrdenId(), a.getTarjetaUltimos4(),
                a.getIntentos(), a.getVentanaSegundos(), a.getCreadoEn());
    }
}

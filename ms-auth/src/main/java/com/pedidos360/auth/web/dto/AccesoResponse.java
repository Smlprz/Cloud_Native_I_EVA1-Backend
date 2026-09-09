package com.pedidos360.auth.web.dto;

import com.pedidos360.auth.domain.Acceso;

import java.time.LocalDateTime;

public record AccesoResponse(
        Long id,
        String usuarioId,
        String nombre,
        String email,
        String roles,
        String ip,
        LocalDateTime creadoEn
) {

    public static AccesoResponse from(Acceso a) {
        return new AccesoResponse(
                a.getId(), a.getUsuarioId(), a.getNombre(), a.getEmail(),
                a.getRoles(), a.getIp(), a.getCreadoEn());
    }
}

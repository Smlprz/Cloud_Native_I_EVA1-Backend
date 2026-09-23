package com.petshop.auth.web.dto;

import com.petshop.auth.domain.Acceso;

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

package com.petshop.resenas.web.dto;

import com.petshop.resenas.domain.Resena;

import java.time.LocalDateTime;

public record ResenaResponse(Long id, Long productoId, String usuarioNombre, int puntaje,
                             String comentario, LocalDateTime creadoEn) {

    public static ResenaResponse from(Resena r) {
        return new ResenaResponse(r.getId(), r.getProductoId(), r.getUsuarioNombre(),
                r.getPuntaje(), r.getComentario(), r.getCreadoEn());
    }
}

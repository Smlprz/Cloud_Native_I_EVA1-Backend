package com.petshop.orden.web.dto;

import com.petshop.orden.domain.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenResponse(Long id, BigDecimal total, String estado, LocalDateTime creadoEn) {

    public static OrdenResponse from(Orden o) {
        return new OrdenResponse(o.getId(), o.getTotal(), o.getEstado(), o.getCreadoEn());
    }
}

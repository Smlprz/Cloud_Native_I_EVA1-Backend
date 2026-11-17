package com.petshop.cupones.web.dto;

import com.petshop.cupones.domain.Cupon;
import com.petshop.cupones.domain.TipoCupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuponResponse(
        Long id,
        String codigo,
        TipoCupon tipo,
        BigDecimal valor,
        BigDecimal montoMinimo,
        LocalDateTime venceEn,
        Integer usosMaximos,
        int usosActuales,
        boolean activo
) {

    public static CuponResponse from(Cupon c) {
        return new CuponResponse(c.getId(), c.getCodigo(), c.getTipo(), c.getValor(), c.getMontoMinimo(),
                c.getVenceEn(), c.getUsosMaximos(), c.getUsosActuales(), c.isActivo());
    }
}

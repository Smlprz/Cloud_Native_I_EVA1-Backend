package com.petshop.pago.web.dto;

import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long ordenId,
        EstadoPago estado,
        String motivo,
        BigDecimal monto,
        String tarjetaUltimos4,
        LocalDateTime creadoEn
) {

    public static PagoResponse from(Pago p) {
        return new PagoResponse(p.getId(), p.getOrdenId(), p.getEstado(), p.getMotivo(),
                p.getMonto(), p.getTarjetaUltimos4(), p.getCreadoEn());
    }
}

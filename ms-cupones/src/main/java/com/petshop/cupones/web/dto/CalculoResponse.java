package com.petshop.cupones.web.dto;

import java.math.BigDecimal;

/** Resultado de validar o aplicar un cupon: cuanto se descuenta y cuanto queda por pagar. */
public record CalculoResponse(String codigo, BigDecimal montoOriginal, BigDecimal descuento, BigDecimal total) {
}

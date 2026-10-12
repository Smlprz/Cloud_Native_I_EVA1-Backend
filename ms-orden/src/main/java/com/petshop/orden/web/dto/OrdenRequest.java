package com.petshop.orden.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrdenRequest(@NotNull @DecimalMin("0.01") BigDecimal total) {
}

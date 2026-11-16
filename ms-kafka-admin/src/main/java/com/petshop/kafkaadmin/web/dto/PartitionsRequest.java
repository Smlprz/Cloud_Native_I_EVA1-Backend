package com.petshop.kafkaadmin.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/kafka/topics/{name}/partitions: nuevo total de particiones (solo se puede aumentar). */
public record PartitionsRequest(@NotNull @Min(1) @Max(100) Integer total) {
}

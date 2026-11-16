package com.petshop.kafkaadmin.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Cuerpo de POST /api/kafka/topics.
 * partitions y replicationFactor son opcionales: si faltan se usan los valores por defecto del cluster (3 y 3).
 * config admite solo las claves de NombresKafka.CONFIGS_PERMITIDAS (retention.ms, cleanup.policy, ...).
 */
public record TopicRequest(
        @NotBlank
        @Size(max = NombresKafka.MAX)
        @Pattern(regexp = NombresKafka.NOMBRE_REGEX, message = NombresKafka.NOMBRE_MENSAJE)
        String name,

        @Min(1) @Max(100)
        Integer partitions,

        @Min(1) @Max(10)
        Short replicationFactor,

        Map<String, String> config
) {
}

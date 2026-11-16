package com.petshop.kafkaadmin.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Cuerpo de PUT /api/kafka/topics/{name}/config: configuraciones a cambiar (retention.ms, cleanup.policy...). */
public record TopicConfigRequest(@NotEmpty Map<String, String> config) {
}

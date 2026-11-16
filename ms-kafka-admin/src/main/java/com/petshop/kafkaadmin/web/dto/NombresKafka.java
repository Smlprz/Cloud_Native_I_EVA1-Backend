package com.petshop.kafkaadmin.web.dto;

import java.util.Set;

/**
 * Reglas de validacion para nombres de topicos, consumer groups y configuraciones.
 * Kafka admite hasta 249 caracteres con letras, numeros y . _ - ; aqui ademas se exige empezar
 * con letra o numero (se rechazan "." , ".." y los topicos internos que empiezan con "_").
 */
public final class NombresKafka {

    public static final int MAX = 249;
    public static final String NOMBRE_REGEX = "^[A-Za-z0-9][A-Za-z0-9._-]*$";
    public static final String NOMBRE_MENSAJE =
            "solo letras, numeros y . _ - (debe empezar con letra o numero)";

    /** Configuraciones de topico que la API permite modificar (lista cerrada: evita tocar parametros peligrosos). */
    public static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "retention.ms",
            "retention.bytes",
            "cleanup.policy",
            "min.insync.replicas",
            "segment.bytes",
            "max.message.bytes",
            "compression.type");

    private NombresKafka() {
    }
}

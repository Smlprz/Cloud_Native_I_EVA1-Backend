package com.petshop.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente de administracion de Kafka. Se conecta a los 3 brokers (bootstrap servers) y falla
 * rapido si el cluster no responde, para que la API devuelva un 503 claro en vez de colgarse.
 */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                   @Value("${kafka.request-timeout-ms}") int requestTimeoutMs,
                                   @Value("${kafka.api-timeout-ms}") int apiTimeoutMs) {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "ms-kafka-admin");
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, apiTimeoutMs);
        props.put(AdminClientConfig.RETRIES_CONFIG, 2);
        return AdminClient.create(props);
    }
}

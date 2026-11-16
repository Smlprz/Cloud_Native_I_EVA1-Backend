package com.petshop.pago.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente productor de Kafka. Conecta a los 3 brokers, serializa a JSON (fechas ISO-8601, sin la cabecera
 * __TypeId__ con el nombre de la clase Java para que cualquier consumidor pueda leerlo) y no pierde
 * mensajes: acks=all + idempotencia. max.block.ms es corto para que un cluster caido no deje colgada la API.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, PagoEvent> pagoProducerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "ms-pago");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonSerializer<PagoEvent> valores = new JsonSerializer<>(mapper);
        valores.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valores);
    }

    @Bean
    public KafkaTemplate<String, PagoEvent> pagoKafkaTemplate(ProducerFactory<String, PagoEvent> pagoProducerFactory) {
        return new KafkaTemplate<>(pagoProducerFactory);
    }
}

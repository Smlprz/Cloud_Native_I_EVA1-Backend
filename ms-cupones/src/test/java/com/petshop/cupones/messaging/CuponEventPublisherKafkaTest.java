package com.petshop.cupones.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato del evento contra un Kafka real (embebido): el mismo JSON que entienden los consumidores de analitica. */
@SpringBootTest(properties = {
        "kafka.publish-enabled=true",
        "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 3, topics = "eventos.navegacion")
@ActiveProfiles("test")
class CuponEventPublisherKafkaTest {

    @Autowired private CuponEventPublisher publicador;
    @Value("${spring.embedded.kafka.brokers}") private String brokers;

    @Test
    void publicaCuponAplicadoComoJsonConLaClaveDelUsuario() throws Exception {
        publicador.cuponAplicado("usuario-8", "VERANO10", new BigDecimal("6598.00"));

        ConsumerRecord<String, String> registro = leerUno("eventos.navegacion");

        assertEquals("usuario-8", registro.key());
        assertNull(registro.headers().lastHeader("__TypeId__"), "no debe llevar el nombre de la clase Java");
        JsonNode json = new ObjectMapper().readTree(registro.value());
        assertEquals("CUPON_APLICADO", json.get("tipo").asText());
        assertEquals("usuario-8", json.get("usuarioId").asText());
        assertEquals(6598.00, json.get("precio").asDouble());
        assertEquals("ms-cupones", json.get("origen").asText());
        assertEquals("cupon VERANO10: -$6598.00", json.get("detalle").asText());
        assertTrue(json.get("productoId").isNull(), "un cupon no es de un producto");
        assertNotNull(json.get("eventId"));
        Instant.parse(json.get("timestamp").asText());
    }

    private ConsumerRecord<String, String> leerUno(String topico) {
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-contrato",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            c.subscribe(List.of(topico));
            long limite = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < limite) {
                for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(500))) {
                    return r;
                }
            }
        }
        throw new AssertionError("No llego ningun mensaje a " + topico);
    }
}

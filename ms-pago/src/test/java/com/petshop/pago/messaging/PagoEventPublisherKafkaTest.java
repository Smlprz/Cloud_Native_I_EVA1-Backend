package com.petshop.pago.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
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

/** Contrato del evento: publica en eventos.pagos un JSON que cualquier consumidor puede leer. */
@SpringBootTest(properties = "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 3, topics = "eventos.pagos")
@ActiveProfiles("test")
class PagoEventPublisherKafkaTest {

    @Autowired private PagoEventPublisher publicador;
    @Value("${spring.embedded.kafka.brokers}") private String brokers;

    @Test
    void publicaElResultadoDelPagoComoJsonSinCabeceraDeTipoJava() throws Exception {
        publicador.publicar(new Pago(42L, "usuario-9", new BigDecimal("1500.50"), "1111", "h".repeat(64),
                EstadoPago.BLOQUEADO_FRAUDE, "tarjeta repetida"));

        ConsumerRecord<String, String> registro = leerUnoDe("eventos.pagos");

        assertEquals("usuario-9", registro.key()); // la clave es el usuario
        assertNull(registro.headers().lastHeader("__TypeId__"), "no debe llevar el nombre de la clase Java");
        JsonNode json = new ObjectMapper().readTree(registro.value());
        assertEquals("FRAUDE_BLOQUEADO", json.get("tipo").asText());
        assertEquals(42, json.get("ordenId").asLong());
        assertEquals("usuario-9", json.get("usuarioId").asText());
        assertEquals(1500.50, json.get("monto").asDouble());
        assertEquals("1111", json.get("tarjetaUltimos4").asText());
        assertEquals("tarjeta repetida", json.get("motivo").asText());
        assertNotNull(json.get("eventId"));
        assertTrue(json.get("timestamp").isTextual(), "la fecha va en ISO-8601, no como numero");
        Instant.parse(json.get("timestamp").asText());
    }

    private ConsumerRecord<String, String> leerUnoDe(String topico) {
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

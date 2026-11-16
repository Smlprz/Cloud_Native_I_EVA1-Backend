package com.petshop.analytics.kafka;

import com.petshop.analytics.service.CatalogoClient;
import com.petshop.analytics.service.MetricasService;
import com.petshop.analytics.service.MetricasSnapshot;
import com.petshop.analytics.service.PerfilService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flujo completo contra un Kafka real (embebido): los consumidores leen de eventos.navegacion y eventos.pagos,
 * actualizan perfiles y metricas, y un mensaje roto o invalido se descarta SIN bloquear los siguientes.
 */
@SpringBootTest(properties = {
        "kafka.listeners-enabled=true",
        "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 3, topics = {"eventos.navegacion", "eventos.pagos"})
@ActiveProfiles("test")
class KafkaConsumersIntegrationTest {

    @Autowired private MetricasService metricas;
    @Autowired private PerfilService perfiles;
    @Autowired private KafkaListenerEndpointRegistry registro;
    @MockBean private CatalogoClient catalogo;
    @Value("${spring.embedded.kafka.brokers}") private String brokers;

    private KafkaProducer<String, String> productor() {
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
    }

    private static String navegacion(String tipo, String usuario, Long productoId, String categoria) {
        return """
                {"eventId":"%s","tipo":"%s","usuarioId":"%s","productoId":%s,"nombreProducto":"Producto %s",
                 "categoria":%s,"precio":1000.00,"cantidad":1,"origen":"ms-test","timestamp":"%s","campoNuevo":"se ignora"}"""
                .formatted(UUID.randomUUID(), tipo, usuario, productoId, productoId,
                        categoria == null ? "null" : "\"" + categoria + "\"", Instant.now());
    }

    private static String pago(String tipo, long ordenId) {
        return """
                {"eventId":"%s","tipo":"%s","ordenId":%d,"usuarioId":"u-pago","monto":5000.00,
                 "tarjetaUltimos4":"4242","motivo":"prueba","timestamp":"%s"}"""
                .formatted(UUID.randomUUID(), tipo, ordenId, Instant.now());
    }

    @Test
    void levantaLosConsumidoresDeLosDosGruposYLasTresParticiones() {
        // 3 listeners: recomendaciones (navegacion), metricas (navegacion) y metricas (pagos)
        assertEquals(3, registro.getListenerContainers().size());
        for (MessageListenerContainer c : registro.getListenerContainers()) {
            await().atMost(Duration.ofSeconds(30)).until(c::isRunning);
        }
    }

    @Test
    void losEventosDeNavegacionActualizanElPerfilYLasMetricas() throws Exception {
        String usuario = "usuario-flujo";
        long antes = metricas.snapshot().totalEventos();
        try (KafkaProducer<String, String> p = productor()) {
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("PRODUCTO_VISTO", usuario, 11L, "Gatos"))).get();
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("PRODUCTO_VISTO", usuario, 11L, "Gatos"))).get();
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("ITEM_AGREGADO", usuario, 11L, "Gatos"))).get();
        }

        // grupo de recomendaciones: 1 + 1 + 3 = 5 puntos en la categoria
        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertEquals(5.0, perfiles.perfilDe(usuario).getCategorias().get("Gatos")));
        // grupo de metricas: leyo los mismos eventos de forma independiente
        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertEquals(antes + 3, metricas.snapshot().totalEventos()));
        assertEquals(2, metricas.vistas(11L));
    }

    @Test
    void unMensajeRotoOInvalidoSeDescartaYLosSiguientesSeSiguenProcesando() throws Exception {
        String usuario = "usuario-errores";
        long descartadosAntes = metricas.snapshot().mensajesDescartados();
        try (KafkaProducer<String, String> p = productor()) {
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, "esto no es json")).get();
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("TIPO_INEXISTENTE", usuario, 12L, "Gatos"))).get();
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("PRODUCTO_VISTO", usuario, null, "Gatos"))).get();
            // este es valido y va DESPUES de los tres defectuosos: debe procesarse igual
            p.send(new ProducerRecord<>("eventos.navegacion", usuario, navegacion("PRODUCTO_VISTO", usuario, 12L, "Perros"))).get();
        }

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertEquals(1.0, perfiles.perfilDe(usuario).getCategorias().get("Perros")));
        // cada uno de los 2 grupos descarta los 3 defectuosos
        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertTrue(metricas.snapshot().mensajesDescartados() >= descartadosAntes + 3,
                        "descartados: " + metricas.snapshot().mensajesDescartados()));
        assertTrue(perfiles.perfilDe(usuario).getCategorias().get("Gatos") == null, "los invalidos no deben sumar");
    }

    @Test
    void losEventosDePagoAlimentanLasMetricasYLasAlertasDeFraude() throws Exception {
        MetricasSnapshot antes = metricas.snapshot();
        try (KafkaProducer<String, String> p = productor()) {
            p.send(new ProducerRecord<>("eventos.pagos", "u-pago", pago("PAGO_APROBADO", 501))).get();
            p.send(new ProducerRecord<>("eventos.pagos", "u-pago", pago("PAGO_RECHAZADO", 502))).get();
            p.send(new ProducerRecord<>("eventos.pagos", "u-pago", pago("FRAUDE_BLOQUEADO", 503))).get();
        }

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
            MetricasSnapshot s = metricas.snapshot();
            assertEquals(antes.pagos().aprobados() + 1, s.pagos().aprobados());
            assertEquals(antes.pagos().rechazados() + 1, s.pagos().rechazados());
            assertEquals(antes.pagos().bloqueadosPorFraude() + 1, s.pagos().bloqueadosPorFraude());
            assertEquals(503L, s.alertasFraude().get(0).ordenId());
        });
    }
}

package com.petshop.analytics.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petshop.analytics.event.EventoInvalidoException;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.event.PagoEvent;
import com.petshop.analytics.service.MetricasService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente consumidor de Kafka.
 *
 *  - Conecta a los 3 brokers. El consumer group lo define cada @KafkaListener (groupId).
 *  - Deserializa JSON a DTO tipados (NavegacionEvent / PagoEvent) ignorando la cabecera de tipo Java y los
 *    campos desconocidos. ErrorHandlingDeserializer evita que un JSON roto bloquee la particion.
 *  - concurrency = 3: cada listener levanta 3 consumidores y se reparten las 3 particiones del topico.
 *  - Confirmacion de offsets registro a registro (AckMode.RECORD): si el servicio cae, retoma desde el ultimo procesado.
 *  - Errores: se reintenta un error tecnico unas veces; un mensaje invalido no se reintenta. En ambos casos, al
 *    agotarse, se registra en el log con su contenido, se cuenta como descartado y se sigue con el siguiente.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private final String bootstrapServers;
    private final String autoOffsetReset;
    private final int maxPollRecords;
    private final int concurrency;
    private final int reintentos;
    private final long esperaMs;

    public KafkaConsumerConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                               @Value("${kafka.consumer.auto-offset-reset}") String autoOffsetReset,
                               @Value("${kafka.consumer.max-poll-records}") int maxPollRecords,
                               @Value("${kafka.consumer.concurrency}") int concurrency,
                               @Value("${kafka.consumer.retry-attempts}") int reintentos,
                               @Value("${kafka.consumer.retry-backoff-ms}") long esperaMs) {
        this.bootstrapServers = bootstrapServers;
        this.autoOffsetReset = autoOffsetReset;
        this.maxPollRecords = maxPollRecords;
        this.concurrency = concurrency;
        this.reintentos = reintentos;
        this.esperaMs = esperaMs;
    }

    @Bean
    public ConsumerFactory<String, NavegacionEvent> navegacionConsumerFactory() {
        return fabrica(NavegacionEvent.class);
    }

    @Bean
    public ConsumerFactory<String, PagoEvent> pagoConsumerFactory() {
        return fabrica(PagoEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NavegacionEvent> navegacionListenerFactory(
            ConsumerFactory<String, NavegacionEvent> navegacionConsumerFactory, DefaultErrorHandler errorHandler) {
        return contenedor(navegacionConsumerFactory, errorHandler);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PagoEvent> pagoListenerFactory(
            ConsumerFactory<String, PagoEvent> pagoConsumerFactory, DefaultErrorHandler errorHandler) {
        return contenedor(pagoConsumerFactory, errorHandler);
    }

    /** Reintenta los errores tecnicos y, al agotarse, registra el mensaje y sigue. */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(MetricasService metricas) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (registro, error) -> descartar(registro, error, metricas),
                new FixedBackOff(esperaMs, reintentos));
        // Un mensaje invalido no mejora al reintentarlo: se salta de inmediato.
        handler.addNotRetryableExceptions(EventoInvalidoException.class);
        return handler;
    }

    private <T> ConsumerFactory<String, T> fabrica(Class<T> tipo) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "ms-analytics-" + tipo.getSimpleName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // useHeadersIfPresent = false: no se confia en la cabecera __TypeId__ del productor.
        JsonDeserializer<T> json = new JsonDeserializer<>(tipo, mapper, false);
        json.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(json));
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> contenedor(
            ConsumerFactory<String, T> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    private static void descartar(ConsumerRecord<?, ?> registro, Exception error, MetricasService metricas) {
        Throwable causa = error instanceof ListenerExecutionFailedException && error.getCause() != null
                ? error.getCause() : error;
        log.error("MENSAJE DESCARTADO topico={} particion={} offset={} clave={} causa=\"{}\" cuerpo={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                causa.getMessage(), cuerpoDe(registro, causa));
        metricas.registrarDescartado();
    }

    /** El contenido que llego: el objeto si se pudo leer, o los bytes originales si no era JSON valido. */
    private static String cuerpoDe(ConsumerRecord<?, ?> registro, Throwable causa) {
        if (registro.value() != null) {
            return registro.value().toString();
        }
        for (Throwable t = causa; t != null; t = t.getCause()) {
            if (t instanceof DeserializationException de && de.getData() != null) {
                String texto = new String(de.getData(), StandardCharsets.UTF_8);
                return texto.length() > 500 ? texto.substring(0, 500) + "..." : texto;
            }
        }
        return "(sin contenido)";
    }
}

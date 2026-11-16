package com.petshop.analytics.kafka;

import com.petshop.analytics.event.EventosValidator;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.service.MetricasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos.navegacion del grupo "analytics-metricas": calcula las metricas en tiempo real del dashboard.
 * Es OTRO grupo que el de recomendaciones: lee el mismo topico de forma independiente (con su propio offset y lag),
 * de modo que una responsabilidad puede ir atrasada o fallar sin afectar a la otra.
 */
@Component
@ConditionalOnProperty(name = "kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class NavegacionMetricasConsumer {

    private static final Logger log = LoggerFactory.getLogger(NavegacionMetricasConsumer.class);

    private final EventosValidator validador;
    private final MetricasService metricas;

    public NavegacionMetricasConsumer(EventosValidator validador, MetricasService metricas) {
        this.validador = validador;
        this.metricas = metricas;
    }

    @KafkaListener(
            id = "navegacion-metricas",
            topics = "${kafka.topics.navegacion}",
            groupId = "${kafka.groups.metricas}",
            containerFactory = "navegacionListenerFactory")
    public void onNavegacion(ConsumerRecord<String, NavegacionEvent> registro) {
        NavegacionEvent evento = registro.value();
        validador.validar(evento);
        metricas.registrar(evento);
        log.debug("[metricas] {} usuario={} particion={} offset={}",
                evento.tipo(), evento.usuarioId(), registro.partition(), registro.offset());
    }
}

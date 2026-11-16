package com.petshop.analytics.kafka;

import com.petshop.analytics.event.EventosValidator;
import com.petshop.analytics.event.PagoEvent;
import com.petshop.analytics.service.MetricasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos.pagos del grupo "analytics-metricas": cuenta los pagos aprobados, rechazados y bloqueados
 * por el antifraude, y guarda las ultimas alertas para el dashboard.
 */
@Component
@ConditionalOnProperty(name = "kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class PagoMetricasConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagoMetricasConsumer.class);

    private final EventosValidator validador;
    private final MetricasService metricas;

    public PagoMetricasConsumer(EventosValidator validador, MetricasService metricas) {
        this.validador = validador;
        this.metricas = metricas;
    }

    @KafkaListener(
            id = "pagos-metricas",
            topics = "${kafka.topics.pagos}",
            groupId = "${kafka.groups.metricas}",
            containerFactory = "pagoListenerFactory")
    public void onPago(ConsumerRecord<String, PagoEvent> registro) {
        PagoEvent evento = registro.value();
        validador.validar(evento);
        metricas.registrarPago(evento);
        if (PagoEvent.FRAUDE_BLOQUEADO.equals(evento.tipo())) {
            log.warn("[metricas] Alerta de fraude recibida: orden={} tarjeta=****{} particion={} offset={}",
                    evento.ordenId(), evento.tarjetaUltimos4(), registro.partition(), registro.offset());
        } else {
            log.debug("[metricas] {} orden={} particion={} offset={}",
                    evento.tipo(), evento.ordenId(), registro.partition(), registro.offset());
        }
    }
}

package com.petshop.analytics.kafka;

import com.petshop.analytics.event.EventosValidator;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.service.PerfilService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos.navegacion del grupo "analytics-recomendaciones": alimenta el perfil de navegacion de cada
 * usuario (y el historial en DynamoDB). Solo recibe, valida y delega: la logica esta en PerfilService.
 *
 * Los 3 consumidores del grupo se reparten las 3 particiones del topico; todos los eventos de un usuario van a la
 * misma particion (la clave del mensaje), asi se procesan en orden y sin carreras sobre su perfil.
 */
@Component
@ConditionalOnProperty(name = "kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class NavegacionRecomendacionesConsumer {

    private static final Logger log = LoggerFactory.getLogger(NavegacionRecomendacionesConsumer.class);

    private final EventosValidator validador;
    private final PerfilService perfiles;

    public NavegacionRecomendacionesConsumer(EventosValidator validador, PerfilService perfiles) {
        this.validador = validador;
        this.perfiles = perfiles;
    }

    @KafkaListener(
            id = "navegacion-recomendaciones",
            topics = "${kafka.topics.navegacion}",
            groupId = "${kafka.groups.recomendaciones}",
            containerFactory = "navegacionListenerFactory")
    public void onNavegacion(ConsumerRecord<String, NavegacionEvent> registro) {
        NavegacionEvent evento = registro.value();
        validador.validar(evento);
        boolean cambio = perfiles.aplicar(evento);
        log.debug("[recomendaciones] {} usuario={} producto={} particion={} offset={} perfilActualizado={}",
                evento.tipo(), evento.usuarioId(), evento.productoId(), registro.partition(), registro.offset(), cambio);
    }
}

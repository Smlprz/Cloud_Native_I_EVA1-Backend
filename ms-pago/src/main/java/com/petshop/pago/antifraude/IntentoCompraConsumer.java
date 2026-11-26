package com.petshop.pago.antifraude;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico pagos.intentos (grupo antifraude): cuenta los intentos de cada usuario y, si pasa
 * el limite, lo bloquea en DynamoDB.
 */
@Component
@ConditionalOnProperty(name = "app.antifraude.kafka-habilitado", havingValue = "true", matchIfMissing = true)
public class IntentoCompraConsumer {

    private final Antifraude antifraude;

    public IntentoCompraConsumer(Antifraude antifraude) {
        this.antifraude = antifraude;
    }

    @KafkaListener(topics = "${kafka.topics.intentos}", groupId = "${app.antifraude.grupo}",
            containerFactory = "intentoListenerFactory")
    public void onIntento(IntentoCompraEvent intento) {
        if (intento != null && intento.usuarioId() != null) {
            antifraude.registrarIntento(intento.usuarioId());
        }
    }
}

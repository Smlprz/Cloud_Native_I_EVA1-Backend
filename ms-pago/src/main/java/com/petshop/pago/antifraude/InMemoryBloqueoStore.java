package com.petshop.pago.antifraude;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Bloqueos en memoria (app.antifraude.store=memory): para desarrollo y pruebas. */
@Component
@ConditionalOnProperty(name = "app.antifraude.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryBloqueoStore implements BloqueoStore {

    private final Map<String, Instant> bloqueos = new ConcurrentHashMap<>();

    @Override
    public Optional<Instant> bloqueadoHasta(String usuarioId, Instant ahora) {
        Instant hasta = bloqueos.get(usuarioId);
        if (hasta == null || !hasta.isAfter(ahora)) {
            bloqueos.remove(usuarioId);
            return Optional.empty();
        }
        return Optional.of(hasta);
    }

    @Override
    public void bloquear(String usuarioId, Instant hasta) {
        bloqueos.put(usuarioId, hasta);
    }

    @Override
    public void desbloquear(String usuarioId) {
        bloqueos.remove(usuarioId);
    }
}

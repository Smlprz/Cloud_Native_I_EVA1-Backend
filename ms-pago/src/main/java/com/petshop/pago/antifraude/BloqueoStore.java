package com.petshop.pago.antifraude;

import java.time.Instant;
import java.util.Optional;

/** Donde se guardan los usuarios bloqueados por el antifraude (DynamoDB en la nube, memoria en local). */
public interface BloqueoStore {

    /** Hasta cuando esta bloqueado el usuario, o vacio si no lo esta. */
    Optional<Instant> bloqueadoHasta(String usuarioId, Instant ahora);

    void bloquear(String usuarioId, Instant hasta);

    void desbloquear(String usuarioId);
}

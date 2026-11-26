package com.petshop.pago.antifraude;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regla: mas de 3 compras en 1 minuto = bloqueo de 15 minutos (evaluado sin Kafka). */
class AntifraudeTest {

    /** Reloj que se puede adelantar. */
    static class RelojMovil extends Clock {
        Instant ahora = Instant.parse("2026-11-26T12:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final RelojMovil reloj = new RelojMovil();
    private final Antifraude antifraude = new Antifraude(new DetectorFraude(3, 60, reloj), new InMemoryBloqueoStore(),
            null, "pagos.intentos", Duration.ofMinutes(15), reloj);

    @Test
    void laCuartaCompraEnUnMinutoBloqueaQuinceMinutos() {
        for (int i = 0; i < 3; i++) {
            assertFalse(antifraude.evaluar("ana", 1L).bloqueado(), "intento " + (i + 1));
        }
        Antifraude.Resultado cuarto = antifraude.evaluar("ana", 1L);

        assertTrue(cuarto.bloqueado());
        assertEquals(reloj.ahora.plus(Duration.ofMinutes(15)), cuarto.hasta());
        assertFalse(antifraude.evaluar("beto", 1L).bloqueado(), "otro usuario no se ve afectado");
    }

    @Test
    void elBloqueoDuraQuinceMinutos() {
        for (int i = 0; i < 4; i++) {
            antifraude.evaluar("ana", 1L);
        }
        reloj.avanzar(Duration.ofMinutes(14));
        assertTrue(antifraude.evaluar("ana", 1L).bloqueado());

        reloj.avanzar(Duration.ofMinutes(2));
        assertFalse(antifraude.evaluar("ana", 1L).bloqueado());
    }

    @Test
    void tresComprasPorMinutoNoSeBloquean() {
        for (int i = 0; i < 9; i++) {
            assertFalse(antifraude.evaluar("ana", 1L).bloqueado(), "intento " + (i + 1));
            reloj.avanzar(Duration.ofSeconds(21));
        }
    }

    @Test
    void unAdminPuedeDesbloquear() {
        for (int i = 0; i < 4; i++) {
            antifraude.evaluar("ana", 1L);
        }
        antifraude.desbloquear("ana");
        assertTrue(antifraude.bloqueadoHasta("ana").isEmpty());
    }
}

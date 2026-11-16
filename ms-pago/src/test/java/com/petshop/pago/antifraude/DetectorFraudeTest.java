package com.petshop.pago.antifraude;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regla: la misma tarjeta MAS de 3 veces en 10 segundos se bloquea. */
class DetectorFraudeTest {

    /** Reloj manual para avanzar el tiempo sin esperar. */
    private static final class RelojManual extends Clock {
        private final AtomicLong millis = new AtomicLong(1_000_000);

        void avanzar(long ms) {
            millis.addAndGet(ms);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public long millis() { return millis.get(); }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }

    private final RelojManual reloj = new RelojManual();
    private final DetectorFraude detector = new DetectorFraude(3, 10, reloj);

    @Test
    void permiteHastaTresIntentosSeguidosYBloqueaElCuarto() {
        assertFalse(detector.registrarIntento("tarjeta-A").bloqueado());
        assertFalse(detector.registrarIntento("tarjeta-A").bloqueado());
        assertFalse(detector.registrarIntento("tarjeta-A").bloqueado());

        DetectorFraude.Resultado cuarto = detector.registrarIntento("tarjeta-A");
        assertTrue(cuarto.bloqueado());
        assertEquals(4, cuarto.intentosEnVentana());
    }

    @Test
    void siSigueInsistiendoContinuaBloqueada() {
        for (int i = 0; i < 4; i++) {
            detector.registrarIntento("tarjeta-B");
        }
        assertTrue(detector.registrarIntento("tarjeta-B").bloqueado());
        assertTrue(detector.registrarIntento("tarjeta-B").bloqueado());
    }

    @Test
    void pasadaLaVentanaDeDiezSegundosVuelveAPermitir() {
        for (int i = 0; i < 4; i++) {
            detector.registrarIntento("tarjeta-C");
        }
        reloj.avanzar(10_001);

        DetectorFraude.Resultado r = detector.registrarIntento("tarjeta-C");
        assertFalse(r.bloqueado());
        assertEquals(1, r.intentosEnVentana());
    }

    @Test
    void losIntentosEspaciadosNoSeAcumulan() {
        // 1 intento cada 4 s: nunca hay mas de 3 en una ventana de 10 s
        for (int i = 0; i < 10; i++) {
            assertFalse(detector.registrarIntento("tarjeta-D").bloqueado(), "intento " + i);
            reloj.avanzar(4_000);
        }
    }

    @Test
    void ventanaDeslizante_unIntentoViejoSaleDeLaCuenta() {
        detector.registrarIntento("tarjeta-E");   // t = 0
        reloj.avanzar(6_000);
        detector.registrarIntento("tarjeta-E");   // t = 6
        detector.registrarIntento("tarjeta-E");   // t = 6
        reloj.avanzar(5_000);                    // t = 11: el primero (t=0) ya salio de la ventana
        DetectorFraude.Resultado r = detector.registrarIntento("tarjeta-E");
        assertFalse(r.bloqueado());
        assertEquals(3, r.intentosEnVentana());
    }

    @Test
    void cadaTarjetaTieneSuPropioContador() {
        for (int i = 0; i < 4; i++) {
            detector.registrarIntento("tarjeta-F");
        }
        assertFalse(detector.registrarIntento("tarjeta-G").bloqueado());
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new DetectorFraude(0, 10, reloj));
        assertThrows(IllegalArgumentException.class, () -> new DetectorFraude(3, 0, reloj));
    }

    @Test
    void esSeguroConHilosConcurrentes() throws Exception {
        DetectorFraude real = new DetectorFraude(3, 10);
        int hilos = 20;
        java.util.concurrent.atomic.AtomicInteger bloqueados = new java.util.concurrent.atomic.AtomicInteger();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(hilos);
        var listos = new java.util.concurrent.CountDownLatch(hilos);
        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                if (real.registrarIntento("tarjeta-H").bloqueado()) {
                    bloqueados.incrementAndGet();
                }
                listos.countDown();
            });
        }
        listos.await();
        pool.shutdown();
        assertEquals(hilos - 3, bloqueados.get()); // exactamente 3 pasan, el resto se bloquea
    }
}

package com.petshop.pago.antifraude;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regla antifraude: si una misma tarjeta se repite MAS de {@code maxIntentos} veces dentro de una ventana
 * deslizante de {@code ventanaSegundos} (por defecto, mas de 3 veces en 10 segundos) el intento se bloquea.
 *
 * Se cuentan todos los intentos de la tarjeta (aprobados, rechazados o bloqueados), de modo que quien sigue
 * insistiendo permanece bloqueado hasta que pasa la ventana. La tarjeta se identifica por su hash, nunca por
 * el numero. El estado vive en memoria de esta instancia (una instancia de ms-pago).
 */
@Component
public class DetectorFraude {

    /** Con mas entradas que esto se descartan las tarjetas sin actividad reciente. */
    private static final int LIMITE_PARA_LIMPIAR = 10_000;

    private final int maxIntentos;
    private final long ventanaMs;
    private final Clock reloj;
    private final Map<String, Deque<Long>> intentosPorTarjeta = new ConcurrentHashMap<>();

    @Autowired
    public DetectorFraude(@Value("${app.antifraude.max-intentos:3}") int maxIntentos,
                          @Value("${app.antifraude.ventana-segundos:10}") int ventanaSegundos) {
        this(maxIntentos, ventanaSegundos, Clock.systemUTC());
    }

    /** Constructor para pruebas: permite controlar el reloj. */
    public DetectorFraude(int maxIntentos, int ventanaSegundos, Clock reloj) {
        if (maxIntentos < 1 || ventanaSegundos < 1) {
            throw new IllegalArgumentException("maxIntentos y ventanaSegundos deben ser positivos");
        }
        this.maxIntentos = maxIntentos;
        this.ventanaMs = ventanaSegundos * 1000L;
        this.reloj = reloj;
    }

    /** Registra un intento con esta tarjeta y dice si debe bloquearse. */
    public Resultado registrarIntento(String tarjetaHash) {
        long ahora = reloj.millis();
        Deque<Long> intentos = intentosPorTarjeta.computeIfAbsent(tarjetaHash, k -> new ArrayDeque<>());
        int enVentana;
        synchronized (intentos) {
            while (!intentos.isEmpty() && intentos.peekFirst() <= ahora - ventanaMs) {
                intentos.pollFirst();
            }
            intentos.addLast(ahora);
            enVentana = intentos.size();
        }
        if (intentosPorTarjeta.size() > LIMITE_PARA_LIMPIAR) {
            limpiar(ahora);
        }
        return new Resultado(enVentana > maxIntentos, enVentana);
    }

    public int getMaxIntentos() {
        return maxIntentos;
    }

    public int getVentanaSegundos() {
        return (int) (ventanaMs / 1000);
    }

    private void limpiar(long ahora) {
        intentosPorTarjeta.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long ultimo = e.getValue().peekLast();
                return ultimo == null || ultimo <= ahora - ventanaMs;
            }
        });
    }

    /** bloqueado = true cuando intentosEnVentana supera el maximo permitido. */
    public record Resultado(boolean bloqueado, int intentosEnVentana) {
    }
}

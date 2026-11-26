package com.petshop.pago.antifraude;

import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Regla antifraude: un usuario con MAS de 3 intentos de compra en 1 minuto queda bloqueado 15 minutos
 * (ANTIFRAUDE_MAX_INTENTOS, ANTIFRAUDE_VENTANA_SEGUNDOS, ANTIFRAUDE_DURACION_BLOQUEO_MINUTOS).
 *
 *   pago -> evaluar(): si el usuario esta bloqueado (DynamoDB) se rechaza; si no, publica el intento en Kafka
 *   Kafka pagos.intentos -> grupo antifraude -> registrarIntento(): cuenta y, si pasa el limite, guarda el bloqueo
 *
 * Kafka es asincrono: el intento que supera el limite puede alcanzar a pasar; desde el siguiente queda bloqueado.
 * Sin Kafka (o si no responde) el intento se cuenta en este mismo proceso. Si DynamoDB no responde no se
 * bloquea a nadie (falla abierta) y queda el error en el log.
 */
@Component
public class Antifraude {

    private static final Logger log = LoggerFactory.getLogger(Antifraude.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Santiago"));

    private final DetectorFraude detector;
    private final BloqueoStore bloqueos;
    private final KafkaTemplate<String, IntentoCompraEvent> kafka;
    private final String topico;
    private final Duration duracionBloqueo;
    private final Clock reloj;

    @Autowired
    public Antifraude(DetectorFraude detector, BloqueoStore bloqueos,
                      ObjectProvider<KafkaTemplate<String, IntentoCompraEvent>> kafka,
                      @Value("${kafka.topics.intentos}") String topico,
                      @Value("${app.antifraude.duracion-bloqueo-minutos:15}") int minutosBloqueo) {
        this(detector, bloqueos, kafka.getIfAvailable(), topico, Duration.ofMinutes(minutosBloqueo), Clock.systemUTC());
    }

    /** kafka = null: sin Kafka, el intento se evalua aqui mismo (pruebas). */
    public Antifraude(DetectorFraude detector, BloqueoStore bloqueos, KafkaTemplate<String, IntentoCompraEvent> kafka,
                      String topico, Duration duracionBloqueo, Clock reloj) {
        this.detector = detector;
        this.bloqueos = bloqueos;
        this.kafka = kafka;
        this.topico = topico;
        this.duracionBloqueo = duracionBloqueo;
        this.reloj = reloj;
    }

    /** Se llama en cada intento de pago. */
    public Resultado evaluar(String usuarioId, Long ordenId) {
        Optional<Instant> hasta = bloqueadoHasta(usuarioId);
        if (hasta.isPresent()) {
            return new Resultado(true, hasta.get());
        }
        if (!publicar(usuarioId, ordenId)) {
            registrarIntento(usuarioId);
            hasta = bloqueadoHasta(usuarioId);
        }
        return new Resultado(hasta.isPresent(), hasta.orElse(null));
    }

    /** Cuenta un intento del usuario y lo bloquea si supera el limite. Lo llama el consumidor de Kafka. */
    public void registrarIntento(String usuarioId) {
        DetectorFraude.Resultado r = detector.registrarIntento(usuarioId);
        Metrics.counter("pedidos360.antifraude.intentos").increment();
        if (!r.bloqueado() || bloqueadoHasta(usuarioId).isPresent()) {
            return;
        }
        Instant hasta = reloj.instant().plus(duracionBloqueo);
        try {
            bloqueos.bloquear(usuarioId, hasta);
            Metrics.counter("pedidos360.antifraude.bloqueos").increment();
            log.warn("ALERTA DE FRAUDE: usuario {} con {} intentos en {} s. Bloqueado hasta {}",
                    usuarioId, r.intentosEnVentana(), detector.getVentanaSegundos(), hasta);
        } catch (Exception e) {
            log.error("No se pudo guardar el bloqueo del usuario {} (no se bloquea): {}", usuarioId, e.getMessage());
        }
    }

    public Optional<Instant> bloqueadoHasta(String usuarioId) {
        try {
            return bloqueos.bloqueadoHasta(usuarioId, reloj.instant());
        } catch (Exception e) {
            log.error("No se pudo consultar el bloqueo del usuario {} (se deja pasar): {}", usuarioId, e.getMessage());
            return Optional.empty();
        }
    }

    public void desbloquear(String usuarioId) {
        bloqueos.desbloquear(usuarioId);
        log.info("Usuario {} desbloqueado por un ADMIN", usuarioId);
    }

    /** Texto para el usuario: por que se bloqueo y hasta que hora. */
    public String motivo(Instant hasta) {
        return "Mas de " + detector.getMaxIntentos() + " intentos de compra en " + detector.getVentanaSegundos()
                + " segundos: usuario bloqueado por seguridad hasta las " + HORA.format(hasta);
    }

    public int getMaxIntentos() {
        return detector.getMaxIntentos();
    }

    public int getVentanaSegundos() {
        return detector.getVentanaSegundos();
    }

    private boolean publicar(String usuarioId, Long ordenId) {
        if (kafka == null) {
            return false;
        }
        try {
            kafka.send(topico, usuarioId, new IntentoCompraEvent(usuarioId, ordenId, reloj.instant()))
                    .whenComplete((ok, error) -> {
                        if (error != null) {
                            log.error("Kafka no recibio el intento del usuario {}: se cuenta localmente ({})",
                                    usuarioId, error.getMessage());
                            registrarIntento(usuarioId);
                        }
                    });
            return true;
        } catch (Exception e) {
            log.error("Kafka no disponible: el intento del usuario {} se cuenta localmente ({})", usuarioId, e.getMessage());
            return false;
        }
    }

    /** bloqueado = true si el usuario esta bloqueado; hasta = fin del bloqueo. */
    public record Resultado(boolean bloqueado, Instant hasta) {
    }
}

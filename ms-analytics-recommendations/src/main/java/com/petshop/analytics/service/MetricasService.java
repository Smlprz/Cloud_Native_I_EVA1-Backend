package com.petshop.analytics.service;

import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.event.PagoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metricas en tiempo real calculadas a medida que llegan los eventos de Kafka (en memoria de esta instancia).
 * El dashboard las recibe por WebSocket cada vez que cambian. Si el servicio se reinicia empiezan de cero;
 * el historial completo queda en el almacen (DynamoDB).
 */
@Service
public class MetricasService {

    private static final int TOP = 5;
    private static final int MAX_ALERTAS = 10;
    /** Ids de evento recordados para ignorar los duplicados que Kafka pueda reentregar. */
    private static final int MAX_IDS_VISTOS = 10_000;

    private final Clock reloj;
    private final Duration ventanaUsuariosActivos;

    private final LongAdder totalEventos = new LongAdder();
    private final Map<String, LongAdder> porTipo = new ConcurrentHashMap<>();
    private final Map<Long, LongAdder> vistasPorProducto = new ConcurrentHashMap<>();
    private final Map<Long, String> nombres = new ConcurrentHashMap<>();
    private final Map<String, Instant> ultimaActividad = new ConcurrentHashMap<>();
    private final Deque<Long> llegadasRecientes = new ArrayDeque<>();
    private final LongAdder aprobados = new LongAdder();
    private final LongAdder rechazados = new LongAdder();
    private final LongAdder bloqueados = new LongAdder();
    private final LongAdder descartados = new LongAdder();
    private final Deque<MetricasSnapshot.AlertaFraude> alertas = new ArrayDeque<>();
    private final Set<String> idsVistos = Collections.newSetFromMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_IDS_VISTOS;
        }
    });
    private final AtomicLong version = new AtomicLong();

    @Autowired
    public MetricasService(@Value("${analytics.metricas.usuarios-activos-minutos:5}") int minutosActivo) {
        this(minutosActivo, Clock.systemUTC());
    }

    public MetricasService(int minutosActivo, Clock reloj) {
        this.ventanaUsuariosActivos = Duration.ofMinutes(minutosActivo);
        this.reloj = reloj;
    }

    public void registrar(NavegacionEvent e) {
        if (yaProcesado(e.eventId())) {
            return;
        }
        totalEventos.increment();
        porTipo.computeIfAbsent(e.tipo(), t -> new LongAdder()).increment();
        ultimaActividad.put(e.usuarioId(), reloj.instant());
        if (NavegacionEvent.PRODUCTO_VISTO.equals(e.tipo()) && e.productoId() != null) {
            vistasPorProducto.computeIfAbsent(e.productoId(), p -> new LongAdder()).increment();
            if (e.nombreProducto() != null) {
                nombres.put(e.productoId(), e.nombreProducto());
            }
        }
        synchronized (llegadasRecientes) {
            llegadasRecientes.addLast(reloj.millis());
        }
        version.incrementAndGet();
    }

    public void registrarPago(PagoEvent e) {
        if (yaProcesado(e.eventId())) {
            return;
        }
        switch (e.tipo()) {
            case PagoEvent.PAGO_APROBADO -> aprobados.increment();
            case PagoEvent.PAGO_RECHAZADO -> rechazados.increment();
            case PagoEvent.FRAUDE_BLOQUEADO -> {
                bloqueados.increment();
                synchronized (alertas) {
                    alertas.addFirst(new MetricasSnapshot.AlertaFraude(
                            e.ordenId(), e.tarjetaUltimos4(), e.motivo(), e.timestamp()));
                    while (alertas.size() > MAX_ALERTAS) {
                        alertas.removeLast();
                    }
                }
            }
            default -> { // el validador ya descarta los tipos desconocidos
            }
        }
        version.incrementAndGet();
    }

    /** Un mensaje invalido que el consumidor salto (ya quedo registrado en el log con su contenido). */
    public void registrarDescartado() {
        descartados.increment();
        version.incrementAndGet();
    }

    /** Cuantas veces se vio un producto (para la popularidad de las recomendaciones). */
    public long vistas(Long productoId) {
        LongAdder v = vistasPorProducto.get(productoId);
        return v == null ? 0 : v.sum();
    }

    /** Aumenta con cada cambio: el difusor del WebSocket solo envia cuando cambia. */
    public long version() {
        return version.get();
    }

    public MetricasSnapshot snapshot() {
        Instant ahora = reloj.instant();

        long ultimoMinuto;
        synchronized (llegadasRecientes) {
            long limite = reloj.millis() - 60_000;
            while (!llegadasRecientes.isEmpty() && llegadasRecientes.peekFirst() < limite) {
                llegadasRecientes.pollFirst();
            }
            ultimoMinuto = llegadasRecientes.size();
        }

        Map<String, Long> tipos = new TreeMap<>();
        porTipo.forEach((tipo, n) -> tipos.put(tipo, n.sum()));

        List<MetricasSnapshot.ProductoVisto> top = vistasPorProducto.entrySet().stream()
                .map(e -> new MetricasSnapshot.ProductoVisto(e.getKey(),
                        nombres.getOrDefault(e.getKey(), "Producto " + e.getKey()), e.getValue().sum()))
                .sorted(Comparator.comparingLong(MetricasSnapshot.ProductoVisto::vistas).reversed()
                        .thenComparing(MetricasSnapshot.ProductoVisto::productoId))
                .limit(TOP)
                .toList();

        Instant corte = ahora.minus(ventanaUsuariosActivos);
        int activos = (int) ultimaActividad.values().stream().filter(t -> t.isAfter(corte)).count();

        List<MetricasSnapshot.AlertaFraude> ultimasAlertas;
        synchronized (alertas) {
            ultimasAlertas = List.copyOf(alertas);
        }

        return new MetricasSnapshot(totalEventos.sum(), ultimoMinuto, tipos, top, activos,
                new MetricasSnapshot.Pagos(aprobados.sum(), rechazados.sum(), bloqueados.sum()),
                ultimasAlertas, descartados.sum(), ahora);
    }

    private boolean yaProcesado(String eventId) {
        synchronized (idsVistos) {
            return !idsVistos.add(eventId);
        }
    }
}

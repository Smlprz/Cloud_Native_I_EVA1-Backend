package com.petshop.analytics.service;

import com.petshop.analytics.Eventos;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.event.PagoEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricasServiceTest {

    /** Reloj manual para avanzar el tiempo sin esperar. */
    private static final class RelojManual extends Clock {
        private final AtomicLong millis = new AtomicLong(Instant.parse("2026-01-01T10:00:00Z").toEpochMilli());

        void avanzarSegundos(long s) {
            millis.addAndGet(s * 1000);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public long millis() { return millis.get(); }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }

    private final RelojManual reloj = new RelojManual();
    private final MetricasService metricas = new MetricasService(5, reloj);

    @Test
    void cuentaLosEventosTotalesYPorTipo() {
        metricas.registrar(Eventos.visto("u1", 1L, "Gatos"));
        metricas.registrar(Eventos.visto("u2", 1L, "Gatos"));
        metricas.registrar(Eventos.navegacion(NavegacionEvent.ITEM_AGREGADO, "u1", 1L, "Gatos"));

        MetricasSnapshot s = metricas.snapshot();
        assertEquals(3, s.totalEventos());
        assertEquals(2, s.eventosPorTipo().get("PRODUCTO_VISTO"));
        assertEquals(1, s.eventosPorTipo().get("ITEM_AGREGADO"));
        assertEquals(3, s.eventosUltimoMinuto());
    }

    @Test
    void armaElTopCincoDeProductosMasVistos() {
        for (long producto = 1; producto <= 7; producto++) {
            for (long vez = 0; vez < producto; vez++) { // el producto N se ve N veces
                metricas.registrar(Eventos.visto("u" + vez, producto, "Gatos"));
            }
        }

        var top = metricas.snapshot().productosMasVistos();

        assertEquals(5, top.size());
        assertEquals(7L, top.get(0).productoId());
        assertEquals(7, top.get(0).vistas());
        assertEquals("Producto 7", top.get(0).nombre());
        assertEquals(3L, top.get(4).productoId());
    }

    @Test
    void losUsuariosActivosSonLosDeLosUltimosCincoMinutos() {
        metricas.registrar(Eventos.visto("u1", 1L, "Gatos"));
        reloj.avanzarSegundos(4 * 60);
        metricas.registrar(Eventos.visto("u2", 1L, "Gatos"));
        assertEquals(2, metricas.snapshot().usuariosActivos());

        reloj.avanzarSegundos(2 * 60); // u1 lleva 6 min sin actividad, u2 lleva 2
        assertEquals(1, metricas.snapshot().usuariosActivos());
    }

    @Test
    void loEventosDelUltimoMinutoDecaenConElTiempo() {
        metricas.registrar(Eventos.visto("u1", 1L, "Gatos"));
        assertEquals(1, metricas.snapshot().eventosUltimoMinuto());

        reloj.avanzarSegundos(61);

        assertEquals(0, metricas.snapshot().eventosUltimoMinuto());
        assertEquals(1, metricas.snapshot().totalEventos());
    }

    @Test
    void cuentaLosPagosYGuardaLasAlertasDeFraude() {
        metricas.registrarPago(Eventos.pago(PagoEvent.PAGO_APROBADO, 1L));
        metricas.registrarPago(Eventos.pago(PagoEvent.PAGO_APROBADO, 2L));
        metricas.registrarPago(Eventos.pago(PagoEvent.PAGO_RECHAZADO, 3L));
        metricas.registrarPago(Eventos.pago(PagoEvent.FRAUDE_BLOQUEADO, 4L));

        MetricasSnapshot s = metricas.snapshot();
        assertEquals(2, s.pagos().aprobados());
        assertEquals(1, s.pagos().rechazados());
        assertEquals(1, s.pagos().bloqueadosPorFraude());
        assertEquals(1, s.alertasFraude().size());
        assertEquals(4L, s.alertasFraude().get(0).ordenId());
        assertEquals("1111", s.alertasFraude().get(0).tarjetaUltimos4());
    }

    @Test
    void soloRecuerdaLasDiezUltimasAlertasLaMasNuevaPrimero() {
        for (long orden = 1; orden <= 12; orden++) {
            metricas.registrarPago(Eventos.pago(PagoEvent.FRAUDE_BLOQUEADO, orden));
        }

        var alertas = metricas.snapshot().alertasFraude();
        assertEquals(10, alertas.size());
        assertEquals(12L, alertas.get(0).ordenId());
        assertEquals(12, metricas.snapshot().pagos().bloqueadosPorFraude());
    }

    @Test
    void ignoraLosEventosDuplicados() {
        NavegacionEvent e = Eventos.visto("u1", 1L, "Gatos");
        metricas.registrar(e);
        metricas.registrar(e);

        var pago = Eventos.pago(PagoEvent.PAGO_APROBADO, 1L);
        metricas.registrarPago(pago);
        metricas.registrarPago(pago);

        assertEquals(1, metricas.snapshot().totalEventos());
        assertEquals(1, metricas.snapshot().pagos().aprobados());
        assertEquals(1, metricas.vistas(1L));
    }

    @Test
    void laVersionSoloCambiaCuandoHayNovedades() {
        long inicial = metricas.version();
        metricas.snapshot();
        assertEquals(inicial, metricas.version());

        metricas.registrar(Eventos.visto("u1", 1L, "Gatos"));
        assertTrue(metricas.version() > inicial);

        long antes = metricas.version();
        metricas.registrarDescartado();
        assertTrue(metricas.version() > antes);
        assertEquals(1, metricas.snapshot().mensajesDescartados());
    }
}

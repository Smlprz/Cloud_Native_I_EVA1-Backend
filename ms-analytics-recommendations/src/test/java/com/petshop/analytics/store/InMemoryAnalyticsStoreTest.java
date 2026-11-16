package com.petshop.analytics.store;

import com.petshop.analytics.Eventos;
import com.petshop.analytics.event.NavegacionEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAnalyticsStoreTest {

    private final InMemoryAnalyticsStore store = new InMemoryAnalyticsStore();

    @Test
    void unEventoDuplicadoNoSeGuardaDosVeces() {
        NavegacionEvent e = Eventos.visto("u1", 1L, "Gatos");

        assertTrue(store.guardarEvento(e));
        assertFalse(store.guardarEvento(e));
        assertEquals(1, store.ultimosEventos("u1", 10).size());
    }

    @Test
    void devuelveLosUltimosEventosDelMasRecienteAlMasAntiguo() {
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            store.guardarEvento(Eventos.navegacion("PRODUCTO_VISTO", "u1", (long) i, "Gatos", t.plusSeconds(i)));
        }

        List<NavegacionEvent> ultimos = store.ultimosEventos("u1", 3);

        assertEquals(3, ultimos.size());
        assertEquals(4L, ultimos.get(0).productoId());
        assertEquals(2L, ultimos.get(2).productoId());
        assertTrue(store.ultimosEventos("otro", 3).isEmpty());
    }

    @Test
    void guardaSoloLosUltimosDoscientosEventosPorUsuario() {
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        for (int i = 0; i < 250; i++) {
            store.guardarEvento(Eventos.navegacion("PRODUCTO_VISTO", "u1", 1L, "Gatos", t.plusSeconds(i)));
        }
        assertEquals(200, store.ultimosEventos("u1", 1000).size());
    }

    @Test
    void elPerfilGuardadoNoCambiaSinVolverAGuardar() {
        PerfilUsuario perfil = store.obtenerPerfil("u1");
        perfil.sumarCategoria("Gatos", 3);
        perfil.registrarVisto(7L);
        perfil.registrarCompra(9L);
        store.guardarPerfil(perfil);

        PerfilUsuario copia = store.obtenerPerfil("u1");
        copia.sumarCategoria("Gatos", 100); // sin guardar

        PerfilUsuario releido = store.obtenerPerfil("u1");
        assertEquals(3.0, releido.getCategorias().get("Gatos"));
        assertEquals(List.of(7L), releido.getVistosRecientes());
        assertTrue(releido.getComprados().contains(9L));
    }

    @Test
    void unUsuarioNuevoTienePerfilVacio() {
        PerfilUsuario perfil = store.obtenerPerfil("nuevo");
        assertTrue(perfil.getCategorias().isEmpty());
        assertTrue(perfil.getVistosRecientes().isEmpty());
    }

    @Test
    void elPerfilRecuerdaSoloLosUltimosDiezVistosYSinRepetir() {
        PerfilUsuario perfil = new PerfilUsuario("u1");
        for (long i = 1; i <= 15; i++) {
            perfil.registrarVisto(i);
        }
        perfil.registrarVisto(10L); // ya estaba: sube al principio, no se duplica

        assertEquals(PerfilUsuario.MAX_VISTOS, perfil.getVistosRecientes().size());
        assertEquals(10L, perfil.getVistosRecientes().get(0));
        assertEquals(1, perfil.getVistosRecientes().stream().filter(v -> v == 10L).count());
    }

    @Test
    void elPuntajeDeUnaCategoriaNuncaBajaDeCero() {
        PerfilUsuario perfil = new PerfilUsuario("u1");
        perfil.sumarCategoria("Gatos", 1);
        perfil.sumarCategoria("Gatos", -5);

        assertFalse(perfil.getCategorias().containsKey("Gatos"));
    }
}

package com.petshop.analytics.service;

import com.petshop.analytics.Eventos;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.store.InMemoryAnalyticsStore;
import com.petshop.analytics.store.PerfilUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilServiceTest {

    private final InMemoryAnalyticsStore store = new InMemoryAnalyticsStore();
    private final CatalogoClient catalogo = mock(CatalogoClient.class);
    private PerfilService service;

    @BeforeEach
    void preparar() {
        service = new PerfilService(store, catalogo);
    }

    @Test
    void cadaTipoDeEventoSumaSuPesoALaCategoria() {
        service.aplicar(Eventos.visto("u1", 1L, "Gatos"));                                            // +1
        service.aplicar(Eventos.navegacion(NavegacionEvent.ITEM_AGREGADO, "u1", 1L, "Gatos"));          // +3
        service.aplicar(Eventos.navegacion(NavegacionEvent.COMPRA_REALIZADA, "u1", 1L, "Gatos"));       // +5
        service.aplicar(Eventos.navegacion(NavegacionEvent.ITEM_ELIMINADO, "u1", 2L, "Gatos"));         // -1

        PerfilUsuario perfil = service.perfilDe("u1");
        assertEquals(8.0, perfil.getCategorias().get("Gatos"));
        assertTrue(perfil.getComprados().contains(1L));
    }

    @Test
    void recuerdaLosProductosVistosRecientementeElUltimoPrimero() {
        service.aplicar(Eventos.visto("u1", 1L, "Gatos"));
        service.aplicar(Eventos.visto("u1", 2L, "Perros"));
        service.aplicar(Eventos.visto("u1", 3L, "Gatos"));

        assertEquals(java.util.List.of(3L, 2L, 1L), service.perfilDe("u1").getVistosRecientes());
    }

    @Test
    void unEventoDuplicadoNoSumaDosVeces() {
        NavegacionEvent e = Eventos.visto("u1", 1L, "Gatos");

        assertTrue(service.aplicar(e));
        assertFalse(service.aplicar(e));

        assertEquals(1.0, service.perfilDe("u1").getCategorias().get("Gatos"));
    }

    @Test
    void losVisitantesAnonimosNoTienenPerfil() {
        assertFalse(service.aplicar(Eventos.visto("anonimo", 1L, "Gatos")));

        assertTrue(service.perfilDe("anonimo").getCategorias().isEmpty());
        assertTrue(store.ultimosEventos("anonimo", 10).isEmpty());
    }

    @Test
    void siElEventoNoTraeCategoriaLaBuscaEnElCatalogo() {
        when(catalogo.buscar(7L)).thenReturn(Optional.of(
                new ProductoCatalogo(7L, "Casa Perro", "d", new BigDecimal("1000"), 5, "Casas", "img")));

        service.aplicar(Eventos.visto("u1", 7L, null));

        assertEquals(1.0, service.perfilDe("u1").getCategorias().get("Casas"));
    }

    @Test
    void siNoSabeLaCategoriaGuardaElEventoPeroNoInventaGustos() {
        when(catalogo.buscar(99L)).thenReturn(Optional.empty());

        assertTrue(service.aplicar(Eventos.visto("u1", 99L, null)));

        assertTrue(service.perfilDe("u1").getCategorias().isEmpty());
        assertEquals(1, store.ultimosEventos("u1", 10).size());
    }

    @Test
    void losCuponesYEnviosSeGuardanPeroNoCambianLosGustos() {
        service.aplicar(Eventos.navegacion(NavegacionEvent.CUPON_APLICADO, "u1", null, null));

        assertTrue(service.perfilDe("u1").getCategorias().isEmpty());
        assertEquals(1, store.ultimosEventos("u1", 10).size());
    }
}

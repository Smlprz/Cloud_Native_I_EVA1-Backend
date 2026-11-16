package com.petshop.analytics.service;

import com.petshop.analytics.Eventos;
import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.store.InMemoryAnalyticsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecomendacionServiceTest {

    private static final ProductoCatalogo ALIMENTO_A = prod(1L, "Alimento Perro A", "Alimento Perros", 10);
    private static final ProductoCatalogo ALIMENTO_B = prod(2L, "Alimento Perro B", "Alimento Perros", 10);
    private static final ProductoCatalogo RASCADOR = prod(3L, "Rascador Gato", "Gatos", 10);
    private static final ProductoCatalogo CASA_SIN_STOCK = prod(4L, "Casa Perro", "Alimento Perros", 0);
    private static final ProductoCatalogo JUGUETE = prod(5L, "Juguete", "Juguetes", 10);

    private final InMemoryAnalyticsStore store = new InMemoryAnalyticsStore();
    private final CatalogoClient catalogo = mock(CatalogoClient.class);
    private final MetricasService metricas = new MetricasService(5);
    private PerfilService perfiles;
    private RecomendacionService service;

    private static ProductoCatalogo prod(Long id, String nombre, String categoria, int stock) {
        return new ProductoCatalogo(id, nombre, "desc", new BigDecimal("1000"), stock, categoria, "img.webp");
    }

    @BeforeEach
    void preparar() {
        List<ProductoCatalogo> todos = List.of(ALIMENTO_A, ALIMENTO_B, RASCADOR, CASA_SIN_STOCK, JUGUETE);
        when(catalogo.catalogo()).thenReturn(todos);
        when(catalogo.buscar(anyLong())).thenAnswer(inv ->
                todos.stream().filter(p -> p.id().equals(inv.getArgument(0))).findFirst());
        perfiles = new PerfilService(store, catalogo);
        service = new RecomendacionService(store, catalogo, metricas, 4);
    }

    @Test
    void recomiendaProductosDeLaCategoriaQueElUsuarioHaEstadoMirando() {
        perfiles.aplicar(Eventos.visto("u1", 1L, "Alimento Perros"));
        perfiles.aplicar(Eventos.visto("u1", 1L, "Alimento Perros"));
        perfiles.aplicar(Eventos.visto("u1", 3L, "Gatos"));

        List<Recomendacion> r = service.recomendar("u1", null, null);

        assertEquals(2L, r.get(0).productoId()); // el otro alimento de perro: la categoria con mas puntaje
        assertEquals("Porque te interesa Alimento Perros", r.get(0).motivo());
    }

    @Test
    void elProductoQueEstaMirandoDaPesoASuCategoriaYNoSeRecomiendaASiMismo() {
        perfiles.aplicar(Eventos.visto("u1", 3L, "Gatos"));

        List<Recomendacion> r = service.recomendar("u1", 1L, null);

        assertEquals(2L, r.get(0).productoId());
        assertEquals("Parecido a Alimento Perro A", r.get(0).motivo());
        assertTrue(r.stream().noneMatch(x -> x.productoId().equals(1L)));
    }

    @Test
    void descartaLosProductosSinStock() {
        perfiles.aplicar(Eventos.visto("u1", 1L, "Alimento Perros"));

        List<Recomendacion> r = service.recomendar("u1", null, null);

        assertTrue(r.stream().noneMatch(x -> x.productoId().equals(4L)));
    }

    @Test
    void noRecomiendaLoQueElUsuarioYaCompro() {
        perfiles.aplicar(Eventos.navegacion(NavegacionEvent.COMPRA_REALIZADA, "u1", 2L, "Alimento Perros"));

        List<Recomendacion> r = service.recomendar("u1", null, null);

        assertFalse(r.isEmpty());
        assertTrue(r.stream().noneMatch(x -> x.productoId().equals(2L)));
    }

    @Test
    void unUsuarioNuevoRecibeLosMasVistos() {
        for (int i = 0; i < 3; i++) {
            metricas.registrar(Eventos.visto("otro" + i, 5L, "Juguetes")); // el juguete es el mas visto
        }
        metricas.registrar(Eventos.visto("otro9", 3L, "Gatos"));

        List<Recomendacion> r = service.recomendar("nuevo", null, null);

        assertEquals(5L, r.get(0).productoId());
        assertEquals("Uno de los productos mas vistos", r.get(0).motivo());
    }

    @Test
    void respetaElLimitePedidoYSuTopeMaximo() {
        assertEquals(2, service.recomendar("nuevo", null, 2).size());
        assertTrue(service.recomendar("nuevo", null, 500).size() <= 20);
        assertEquals(4, service.recomendar("nuevo", null, null).size()); // limite por defecto = 4
    }

    @Test
    void sinCatalogoNoRecomiendaNadaYNoFalla() {
        when(catalogo.catalogo()).thenReturn(List.of());
        when(catalogo.buscar(anyLong())).thenReturn(Optional.empty());

        assertTrue(service.recomendar("u1", 1L, null).isEmpty());
    }

    @Test
    void laCategoriaConMasPuntajeApareceAntes() {
        // el usuario compra juguetes (5) y solo mira gatos (1): primero juguetes
        perfiles.aplicar(Eventos.navegacion(NavegacionEvent.COMPRA_REALIZADA, "u1", 99L, "Juguetes"));
        perfiles.aplicar(Eventos.visto("u1", 3L, "Gatos"));
        when(catalogo.catalogo()).thenReturn(List.of(RASCADOR, prod(6L, "Otro Juguete", "Juguetes", 3)));

        List<Recomendacion> r = service.recomendar("u1", null, null);

        assertEquals(6L, r.get(0).productoId());
        assertEquals(3L, r.get(1).productoId());
        assertTrue(r.get(0).puntaje() > r.get(1).puntaje());
    }
}

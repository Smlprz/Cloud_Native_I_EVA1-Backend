package com.petshop.analytics.web;

import com.petshop.analytics.service.MetricasService;
import com.petshop.analytics.service.MetricasSnapshot;
import com.petshop.analytics.service.PerfilService;
import com.petshop.analytics.service.Recomendacion;
import com.petshop.analytics.service.RecomendacionService;
import com.petshop.analytics.store.PerfilUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private RecomendacionService recomendaciones;
    @MockBean private PerfilService perfiles;
    @MockBean private MetricasService metricas;

    private static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.claim("oid", "usuario-1"))
                .authorities(new SimpleGrantedAuthority("SCOPE_productos.read"), new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_productos.read"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/analytics/recomendaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/analytics/metricas")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinElScopeCorrectoResponde403() throws Exception {
        mvc.perform(get("/api/analytics/recomendaciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elClienteRecibeSusRecomendacionesUsandoSuIdentidadDelToken() throws Exception {
        when(recomendaciones.recomendar(eq("usuario-1"), eq(7L), any())).thenReturn(List.of(
                new Recomendacion(2L, "Alimento Perro B", "desc", new BigDecimal("1000"), "Alimento Perros", "img.webp",
                        "Parecido a Alimento Perro A", 2.5)));

        mvc.perform(get("/api/analytics/recomendaciones").param("productoId", "7").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(2))
                .andExpect(jsonPath("$[0].motivo").value("Parecido a Alimento Perro A"));
        verify(recomendaciones).recomendar("usuario-1", 7L, null);
    }

    @Test
    void validaLosParametros() throws Exception {
        mvc.perform(get("/api/analytics/recomendaciones").param("limite", "500").with(cliente()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/analytics/recomendaciones").param("productoId", "-3").with(cliente()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elClienteVeSuPerfil() throws Exception {
        PerfilUsuario perfil = new PerfilUsuario("usuario-1");
        perfil.sumarCategoria("Gatos", 4);
        perfil.registrarVisto(3L);
        when(perfiles.perfilDe("usuario-1")).thenReturn(perfil);

        mvc.perform(get("/api/analytics/perfil").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias.Gatos").value(4.0))
                .andExpect(jsonPath("$.vistosRecientes[0]").value(3));
    }

    @Test
    void lasMetricasSonSoloParaElAdmin() throws Exception {
        when(metricas.snapshot()).thenReturn(new MetricasSnapshot(5, 2, Map.of("PRODUCTO_VISTO", 5L), List.of(), 1,
                new MetricasSnapshot.Pagos(1, 0, 0), List.of(), 0, Instant.now()));

        mvc.perform(get("/api/analytics/metricas").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/metricas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(5))
                .andExpect(jsonPath("$.pagos.aprobados").value(1));
    }
}

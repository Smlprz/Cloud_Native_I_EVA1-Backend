package com.petshop.cupones.web;

import com.petshop.cupones.messaging.CuponEventPublisher;
import com.petshop.cupones.service.CuponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API real (con la base H2): seguridad, validaciones, codigos HTTP y el evento hacia Kafka. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CuponControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private CuponService service;
    @MockBean private CuponEventPublisher eventos;

    private static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.claim("oid", "cliente-1"))
                .authorities(new SimpleGrantedAuthority("SCOPE_carrito.write"), new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.claim("oid", "admin-1"))
                .authorities(new SimpleGrantedAuthority("SCOPE_carrito.write"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @BeforeEach
    void cuponDePrueba() {
        // un codigo distinto por prueba evita depender del orden de ejecucion
    }

    private String crearComoAdmin(String codigo, String extra) throws Exception {
        mvc.perform(post("/api/cupones").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"tipo\":\"PORCENTAJE\",\"valor\":10" + extra + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(codigo.toUpperCase()));
        return codigo.toUpperCase();
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post("/api/cupones/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ABC\",\"monto\":1000}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cupones")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloElAdminAdministraCupones() throws Exception {
        mvc.perform(get("/api/cupones").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(post("/api/cupones").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HACKER\",\"tipo\":\"PORCENTAJE\",\"valor\":100}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/cupones").with(admin())).andExpect(status().isOk());
    }

    @Test
    void validaLosDatosAlCrear() throws Exception {
        mvc.perform(post("/api/cupones").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"a\",\"tipo\":\"PORCENTAJE\",\"valor\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cupones").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"BUENCODIGO\",\"tipo\":\"PORCENTAJE\",\"valor\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Un cupon de porcentaje no puede superar el 100 %"));
    }

    @Test
    void elClienteValidaUnCuponSinConsumirloNiPublicarEventos() throws Exception {
        String codigo = crearComoAdmin("validable", "");

        mvc.perform(post("/api/cupones/validar").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo.toLowerCase() + "\",\"monto\":65980}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descuento").value(6598.0))
                .andExpect(jsonPath("$.total").value(59382.0));
        verify(eventos, never()).cuponAplicado(any(), any(), any());
    }

    @Test
    void aplicarConsumeElCuponYPublicaElEventoUnaSolaVez() throws Exception {
        String codigo = crearComoAdmin("aplicable", "");
        String cuerpo = "{\"codigo\":\"" + codigo + "\",\"monto\":10000}";

        mvc.perform(post("/api/cupones/aplicar").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descuento").value(1000.0));
        verify(eventos).cuponAplicado(eq("cliente-1"), eq(codigo), eq(new BigDecimal("1000.00")));

        // segundo intento del mismo usuario: 409 y no se vuelve a publicar
        mvc.perform(post("/api/cupones/aplicar").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya usaste este cupon"));
        verify(eventos).cuponAplicado(any(), any(), any()); // sigue siendo 1 sola llamada
    }

    @Test
    void unCuponInexistenteDevuelve404YUnoDesactivadoTambien() throws Exception {
        mvc.perform(post("/api/cupones/validar").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NOEXISTE\",\"monto\":1000}"))
                .andExpect(status().isNotFound());

        String codigo = crearComoAdmin("apagable", "");
        String id = service.listar().stream().filter(c -> c.codigo().equals(codigo)).findFirst().orElseThrow().id().toString();
        mvc.perform(put("/api/cupones/" + id + "/activo").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post("/api/cupones/validar").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"monto\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaElCuerpoDeValidarYAplicar() throws Exception {
        mvc.perform(post("/api/cupones/validar").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ab\",\"monto\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cupones/aplicar").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isBadRequest());
    }
}

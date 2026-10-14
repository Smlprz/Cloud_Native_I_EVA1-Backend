package com.petshop.orden.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/ordenes")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinRolDevuelve403() throws Exception {
        mvc.perform(get("/api/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ordenes.read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteCreaYListaOrdenes() throws Exception {
        var auth = jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_ordenes.read"),
                new SimpleGrantedAuthority("SCOPE_ordenes.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mvc.perform(post("/api/ordenes").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\": 15990}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/ordenes").with(auth)).andExpect(status().isOk());

        org.mockito.Mockito.verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("pedidos.exchange"),
                org.mockito.ArgumentMatchers.eq("orden.creada"),
                org.mockito.ArgumentMatchers.any(com.petshop.orden.messaging.OrdenCreadaEvent.class));
    }

    @Test
    void elCorreoIndicadoEnLaPeticionTienePrioridadSobreElDelToken() throws Exception {
        var auth = jwt().jwt(j -> j.claim("email", "token@correo.cl")).authorities(
                new SimpleGrantedAuthority("SCOPE_ordenes.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mvc.perform(post("/api/ordenes").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\": 500, \"email\": \"comprobante@destino.cl\"}"))
                .andExpect(status().isCreated());

        var captor = org.mockito.ArgumentCaptor.forClass(com.petshop.orden.messaging.OrdenCreadaEvent.class);
        org.mockito.Mockito.verify(rabbitTemplate, org.mockito.Mockito.atLeastOnce()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("pedidos.exchange"),
                org.mockito.ArgumentMatchers.eq("orden.creada"),
                captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("comprobante@destino.cl", captor.getValue().email());
    }

    @Test
    void sinCorreoEnLaPeticionUsaElDelToken() throws Exception {
        var auth = jwt().jwt(j -> j.claim("email", "token@correo.cl")).authorities(
                new SimpleGrantedAuthority("SCOPE_ordenes.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mvc.perform(post("/api/ordenes").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\": 700}"))
                .andExpect(status().isCreated());

        var captor = org.mockito.ArgumentCaptor.forClass(com.petshop.orden.messaging.OrdenCreadaEvent.class);
        org.mockito.Mockito.verify(rabbitTemplate, org.mockito.Mockito.atLeastOnce()).convertAndSend(
                org.mockito.ArgumentMatchers.eq("pedidos.exchange"),
                org.mockito.ArgumentMatchers.eq("orden.creada"),
                captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("token@correo.cl", captor.getValue().email());
    }

    @Test
    void unCorreoInvalidoDevuelve400() throws Exception {
        var auth = jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_ordenes.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mvc.perform(post("/api/ordenes").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\": 700, \"email\": \"esto-no-es-un-correo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenConScopesDeCarritoTambienPuedeCrearOrdenes() throws Exception {
        var auth = jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_carrito.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));

        mvc.perform(post("/api/ordenes").with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\": 100}"))
                .andExpect(status().isCreated());
    }
}

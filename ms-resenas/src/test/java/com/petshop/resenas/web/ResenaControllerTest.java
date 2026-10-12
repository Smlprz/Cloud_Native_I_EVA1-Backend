package com.petshop.resenas.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResenaControllerTest {

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor cliente(String oid) {
        return jwt().jwt(j -> j.claim("oid", oid).claim("name", "Cliente " + oid)).authorities(
                new SimpleGrantedAuthority("SCOPE_resenas.read"),
                new SimpleGrantedAuthority("SCOPE_resenas.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/resenas").param("productoId", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinScopeDevuelve403() throws Exception {
        mvc.perform(get("/api/resenas").param("productoId", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteCreaYListaResenas() throws Exception {
        mvc.perform(post("/api/resenas").with(cliente("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\": 1, \"puntaje\": 5, \"comentario\": \"Excelente\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/resenas").param("productoId", "1").with(cliente("u1")))
                .andExpect(status().isOk());
    }

    @Test
    void puntajeFueraDeRangoDevuelve400() throws Exception {
        mvc.perform(post("/api/resenas").with(cliente("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\": 1, \"puntaje\": 9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloElAutorPuedeBorrar() throws Exception {
        String respuesta = mvc.perform(post("/api/resenas").with(cliente("autor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\": 2, \"puntaje\": 4}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = respuesta.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(delete("/api/resenas/" + id).with(cliente("otro"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/resenas/" + id).with(cliente("autor"))).andExpect(status().isNoContent());
    }
}

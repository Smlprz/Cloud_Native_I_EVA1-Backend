package com.pedidos360.carrito.web;

import com.pedidos360.carrito.client.ProductosClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el filtro de validacion de JWT y la autorizacion por scope
 * (claim "scp") para el microservicio de Carrito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarritoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductosClient productosClient;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenPeroSinElScopeRequeridoDevuelve403() throws Exception {
        // jwt() por defecto trae SCOPE_read / SCOPE_write, no carrito.read
        mockMvc.perform(get("/api/carrito").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void conScopeYRolDevuelve200() throws Exception {
        mockMvc.perform(get("/api/carrito").with(jwt()
                        .jwt(builder -> builder.claim("oid", "user-xyz"))
                        .authorities(
                                new SimpleGrantedAuthority("SCOPE_carrito.read"),
                                new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void conScopeCarritoReadPeroSinRolDevuelve403() throws Exception {
        mockMvc.perform(get("/api/carrito")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_carrito.read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturaConSoloScopeDeEscrituraDevuelve403() throws Exception {
        mockMvc.perform(get("/api/carrito")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_carrito.write"))))
                .andExpect(status().isForbidden());
    }
}

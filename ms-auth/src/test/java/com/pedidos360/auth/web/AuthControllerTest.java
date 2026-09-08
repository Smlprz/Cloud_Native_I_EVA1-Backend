package com.pedidos360.auth.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-auth"));
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meConTokenDevuelvePerfilYRegistraElAcceso() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(jwt()
                        .jwt(b -> b.claim("oid", "user-123")
                                .claim("name", "Vicente Oyarzun")
                                .claim("preferred_username", "vicente@instituto.cl"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("SCOPE_carrito.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.nombre").value("Vicente Oyarzun"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.scopes[0]").value("carrito.read"));

        // el acceso quedo auditado -> /accesos lo devuelve
        mockMvc.perform(get("/api/auth/accesos").with(jwt()
                        .jwt(b -> b.claim("oid", "user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value("user-123"));
    }
}

package com.pedidos360.productos.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas basicas del catalogo + del filtro de validacion de JWT:
 * el GET es publico, y las escrituras exigen un token con el scope productos.write.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String NUEVO_PRODUCTO = """
            {
              "nombre": "Juguete mordedor",
              "descripcion": "Goma resistente",
              "precio": 4990.00,
              "stock": 25,
              "categoria": "Juguetes",
              "imagenUrl": "juguete.webp"
            }
            """;

    @Test
    void listarCatalogoEsPublico() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType("application/json")
                        .content(NUEVO_PRODUCTO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConTokenSinScopeDeEscrituraDevuelve403() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_productos.read")))
                        .contentType("application/json")
                        .content(NUEVO_PRODUCTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConScopeDeEscrituraPeroSinRolDevuelve403() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_productos.write")))
                        .contentType("application/json")
                        .content(NUEVO_PRODUCTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConScopeYRolVendedorDevuelve201() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_productos.write"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json")
                        .content(NUEVO_PRODUCTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Juguete mordedor"));
    }
}

package com.petshop.carrito.web;

import com.petshop.carrito.client.OrdenesClient;
import com.petshop.carrito.client.ProductosClient;
import com.petshop.carrito.service.CarritoService;
import com.petshop.carrito.web.dto.PedidoResponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El checkout acepta un correo opcional y se lo pasa a ms-orden. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarritoCheckoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarritoService service;

    @MockBean
    private OrdenesClient ordenes;

    @MockBean
    private ProductosClient productosClient;

    private static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.claim("oid", "user-1")).authorities(
                new SimpleGrantedAuthority("SCOPE_carrito.write"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static PedidoResponse pedido() {
        return new PedidoResponse(9L, new BigDecimal("1000"), "CONFIRMADO", LocalDateTime.now(), List.of());
    }

    @Test
    void checkoutConCorreoLoPasaAOrdenes() throws Exception {
        when(service.checkout(anyString())).thenReturn(pedido());

        mockMvc.perform(post("/api/carrito/checkout").with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"comprobante@destino.cl\"}"))
                .andExpect(status().isCreated());

        verify(ordenes).registrarOrden(anyString(), any(PedidoResponse.class), eq("comprobante@destino.cl"));
    }

    @Test
    void checkoutSinCuerpoSigueFuncionandoYNoIndicaCorreo() throws Exception {
        when(service.checkout(anyString())).thenReturn(pedido());

        mockMvc.perform(post("/api/carrito/checkout").with(cliente()))
                .andExpect(status().isCreated());

        verify(ordenes).registrarOrden(anyString(), any(PedidoResponse.class), isNull());
    }

    @Test
    void unCorreoInvalidoDevuelve400YNoConfirmaLaCompra() throws Exception {
        mockMvc.perform(post("/api/carrito/checkout").with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"esto-no-es-un-correo\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).checkout(anyString());
        verify(ordenes, never()).registrarOrden(anyString(), any(), any());
    }
}

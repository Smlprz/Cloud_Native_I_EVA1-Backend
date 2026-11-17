package com.petshop.carrito.web;

import com.petshop.carrito.client.CuponesClient;
import com.petshop.carrito.client.OrdenesClient;
import com.petshop.carrito.client.ProductosClient;
import com.petshop.carrito.service.CarritoService;
import com.petshop.carrito.service.ReglaNegocioException;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El checkout acepta un cupon opcional y lo pasa al servicio junto con el JWT del usuario. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarritoCheckoutCuponTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CarritoService service;
    @MockBean private OrdenesClient ordenes;
    @MockBean private ProductosClient productosClient;
    @MockBean private CuponesClient cupones;

    private static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.claim("oid", "user-1").tokenValue("jwt-del-usuario")).authorities(
                new SimpleGrantedAuthority("SCOPE_carrito.write"), new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static PedidoResponse pedidoConCupon() {
        return new PedidoResponse(9L, new BigDecimal("59382.00"), "CONFIRMADO", LocalDateTime.now(), List.of(),
                new BigDecimal("6598.00"), "VERANO10");
    }

    @Test
    void conCuponLoPasaAlServicioConElTokenDelUsuario() throws Exception {
        when(service.checkout(eq("user-1"), eq("VERANO10"), anyString())).thenReturn(pedidoConCupon());

        mockMvc.perform(post("/api/carrito/checkout").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cupon\": \"VERANO10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(59382.0))
                .andExpect(jsonPath("$.descuento").value(6598.0))
                .andExpect(jsonPath("$.cuponCodigo").value("VERANO10"));

        verify(service).checkout("user-1", "VERANO10", "jwt-del-usuario");
        verify(service, never()).checkout("user-1");
    }

    @Test
    void sinCuponUsaElCheckoutDeSiempre() throws Exception {
        when(service.checkout("user-1")).thenReturn(new PedidoResponse(9L, new BigDecimal("1000"), "CONFIRMADO",
                LocalDateTime.now(), List.of()));

        mockMvc.perform(post("/api/carrito/checkout").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a@b.cl\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descuento").value(0));

        verify(service).checkout("user-1");
    }

    @Test
    void unCuponRechazadoDevuelve400ConElMotivoYNoRegistraLaOrden() throws Exception {
        when(service.checkout(eq("user-1"), eq("VENCIDO"), anyString()))
                .thenThrow(new ReglaNegocioException("El cupon esta vencido"));

        mockMvc.perform(post("/api/carrito/checkout").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cupon\": \"VENCIDO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cupon esta vencido"));

        verify(ordenes, never()).registrarOrden(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validaElFormatoDelCodigoDelCupon() throws Exception {
        mockMvc.perform(post("/api/carrito/checkout").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cupon\": \"a b!\"}"))
                .andExpect(status().isBadRequest());
    }
}

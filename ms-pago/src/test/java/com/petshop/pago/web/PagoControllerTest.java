package com.petshop.pago.web;

import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.service.PagoException;
import com.petshop.pago.service.PagoService;
import com.petshop.pago.web.dto.PagoResponse;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagoControllerTest {

    private static final String CUERPO_VALIDO =
            "{\"ordenId\":7,\"numeroTarjeta\":\"4111 1111 1111 1111\",\"titular\":\"Juan Perez\",\"vencimiento\":\"12/35\",\"cvv\":\"123\"}";

    @Autowired private MockMvc mvc;
    @MockBean private PagoService service;

    private static RequestPostProcessor cliente() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_ordenes.write"), new SimpleGrantedAuthority("SCOPE_ordenes.read"),
                new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_ordenes.read"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static PagoResponse respuesta(EstadoPago estado, String motivo) {
        return new PagoResponse(1L, 7L, estado, motivo, new BigDecimal("1000.00"), "1111", LocalDateTime.now());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post("/api/pagos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinElScopeCorrectoResponde403() throws Exception {
        mvc.perform(post("/api/pagos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void unPagoAprobadoResponde201() throws Exception {
        when(service.procesar(any(), any(), any())).thenReturn(respuesta(EstadoPago.APROBADO, null));

        mvc.perform(post("/api/pagos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.tarjetaUltimos4").value("1111"));
    }

    @Test
    void unPagoRechazadoResponde402ConElMotivo() throws Exception {
        when(service.procesar(any(), any(), any())).thenReturn(respuesta(EstadoPago.RECHAZADO, "Fondos insuficientes"));

        mvc.perform(post("/api/pagos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.detail").value("Fondos insuficientes"))
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));
    }

    @Test
    void unPagoBloqueadoPorFraudeResponde429() throws Exception {
        when(service.procesar(any(), any(), any())).thenReturn(respuesta(EstadoPago.BLOQUEADO_FRAUDE, "bloqueada por seguridad"));

        mvc.perform(post("/api/pagos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO_FRAUDE"));
    }

    @Test
    void unaOrdenYaPagadaResponde409() throws Exception {
        when(service.procesar(any(), any(), any())).thenThrow(PagoException.ordenYaPagada(7L));

        mvc.perform(post("/api/pagos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void validaLosDatosYNuncaDevuelveElNumeroDeTarjeta() throws Exception {
        String invalido = "{\"ordenId\":7,\"numeroTarjeta\":\"1234\",\"titular\":\"\",\"vencimiento\":\"13/99\",\"cvv\":\"1\"}";

        mvc.perform(post("/api/pagos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("numeroTarjeta")))
                .andExpect(content().string(not(containsString("1234"))));
    }

    @Test
    void soloElAdminVeLasAlertasDeFraude() throws Exception {
        when(service.alertasDeFraude()).thenReturn(List.of());

        mvc.perform(get("/api/pagos/alertas-fraude").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(get("/api/pagos/alertas-fraude").with(admin())).andExpect(status().isOk());
    }

    @Test
    void elClienteListaSusPagos() throws Exception {
        when(service.listar(any(), any())).thenReturn(List.of(respuesta(EstadoPago.APROBADO, null)));

        mvc.perform(get("/api/pagos").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ordenId").value(7));
    }
}

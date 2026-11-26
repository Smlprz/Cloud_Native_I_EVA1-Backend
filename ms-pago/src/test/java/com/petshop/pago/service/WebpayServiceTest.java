package com.petshop.pago.service;

import com.petshop.pago.antifraude.Antifraude;
import com.petshop.pago.antifraude.DetectorFraude;
import com.petshop.pago.antifraude.InMemoryBloqueoStore;
import com.petshop.pago.client.OrdenDto;
import com.petshop.pago.client.OrdenesClient;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import com.petshop.pago.domain.WebpayTransaccion;
import com.petshop.pago.messaging.PagoEventPublisher;
import com.petshop.pago.repository.AlertaFraudeRepository;
import com.petshop.pago.repository.PagoRepository;
import com.petshop.pago.repository.WebpayTransaccionRepository;
import com.petshop.pago.webpay.WebpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebpayServiceTest {

    private final WebpayClient webpay = mock(WebpayClient.class);
    private final WebpayTransaccionRepository transacciones = mock(WebpayTransaccionRepository.class);
    private final PagoRepository pagos = mock(PagoRepository.class);
    private final AlertaFraudeRepository alertas = mock(AlertaFraudeRepository.class);
    private final OrdenesClient ordenes = mock(OrdenesClient.class);
    private final PagoEventPublisher publicador = mock(PagoEventPublisher.class);
    private WebpayService service;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        Antifraude antifraude = new Antifraude(new DetectorFraude(3, 60, reloj), new InMemoryBloqueoStore(),
                null, "pagos.intentos", Duration.ofMinutes(15), reloj);
        service = new WebpayService(webpay, transacciones, pagos, alertas, ordenes, antifraude, publicador,
                "pepper", "http://localhost:3000/pago/retorno");
        when(ordenes.buscar(anyLong(), anyString()))
                .thenReturn(Optional.of(new OrdenDto(7L, new BigDecimal("5000.00"), "CREADA")));
        when(webpay.crear(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new WebpayClient.Creada("tok", "https://webpay/form"));
        when(pagos.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void elCuartoIntentoDelMismoUsuarioSeBloquea() {
        for (int i = 0; i < 3; i++) {
            service.iniciar("ana", "jwt", 7L);
        }
        PagoException e = assertThrows(PagoException.class, () -> service.iniciar("ana", "jwt", 7L));
        assertEquals(429, e.getStatus().value());
        verify(alertas).save(any());
        verify(webpay, times(3)).crear(anyString(), anyString(), anyLong(), anyString());
        // otro usuario no se ve afectado
        assertEquals("tok", service.iniciar("beto", "jwt", 7L).token());
    }

    @Test
    void confirmarAprobadoRegistraYPublicaElPago() {
        when(transacciones.findById("tok")).thenReturn(Optional.of(
                new WebpayTransaccion("tok", 7L, "ana", new BigDecimal("5000.00"), "O7-1")));
        when(webpay.confirmar("tok")).thenReturn(new WebpayClient.Confirmacion(
                "AUTHORIZED", 0, "O7-1", 5000, new WebpayClient.Tarjeta("6623")));

        assertEquals(EstadoPago.APROBADO, service.confirmar("ana", "tok").estado());
        verify(publicador).publicar(any());
    }

    @Test
    void confirmarConOtroMontoSeRechaza() {
        when(transacciones.findById("tok")).thenReturn(Optional.of(
                new WebpayTransaccion("tok", 7L, "ana", new BigDecimal("5000.00"), "O7-1")));
        when(webpay.confirmar("tok")).thenReturn(new WebpayClient.Confirmacion("AUTHORIZED", 0, "O7-1", 1, null));

        assertEquals(EstadoPago.RECHAZADO, service.confirmar("ana", "tok").estado());
    }

    @Test
    void noSePuedeConfirmarLaTransaccionDeOtroUsuario() {
        when(transacciones.findById("tok")).thenReturn(Optional.of(
                new WebpayTransaccion("tok", 7L, "ana", new BigDecimal("5000.00"), "O7-1")));

        PagoException e = assertThrows(PagoException.class, () -> service.confirmar("beto", "tok"));
        assertEquals(404, e.getStatus().value());
    }
}

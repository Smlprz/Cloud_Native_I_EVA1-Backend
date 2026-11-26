package com.petshop.pago.service;

import com.petshop.pago.client.OrdenDto;
import com.petshop.pago.client.OrdenesClient;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.messaging.PagoEventPublisher;
import com.petshop.pago.repository.AlertaFraudeRepository;
import com.petshop.pago.repository.PagoRepository;
import com.petshop.pago.web.dto.PagoRequest;
import com.petshop.pago.web.dto.PagoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Flujo completo del pago con la base de datos (H2). ms-orden y Kafka se reemplazan por dobles.
 * Cada prueba usa un usuario distinto: el antifraude es un singleton y recuerda los intentos de cada usuario.
 */
@SpringBootTest
@ActiveProfiles("test")
class PagoServiceTest {

    private final String USUARIO = "usuario-" + java.util.UUID.randomUUID();
    private static final String TOKEN = "token";

    @Autowired private PagoService service;
    @Autowired private PagoRepository pagos;
    @Autowired private AlertaFraudeRepository alertas;

    @MockBean private OrdenesClient ordenes;
    @MockBean private PagoEventPublisher publicador;

    private long siguienteOrden = 100;

    private Long ordenExistente(String total) {
        Long id = siguienteOrden++;
        when(ordenes.buscar(eq(id), any())).thenReturn(Optional.of(new OrdenDto(id, new BigDecimal(total), "CREADA")));
        return id;
    }

    private static PagoRequest pago(Long ordenId, String tarjeta) {
        return new PagoRequest(ordenId, tarjeta, "Juan Perez", "12/35", "123");
    }

    @BeforeEach
    void limpiar() {
        pagos.deleteAll();
        alertas.deleteAll();
    }

    @Test
    void apruebaUnaTarjetaValidaYGuardaSoloLosUltimosCuatroDigitos() {
        Long orden = ordenExistente("65980.00");

        PagoResponse r = service.procesar(USUARIO, TOKEN, pago(orden, "4111 1111 1111 1111"));

        assertEquals(EstadoPago.APROBADO, r.estado());
        assertEquals(new BigDecimal("65980.00"), r.monto()); // el monto sale de la orden
        assertEquals("1111", r.tarjetaUltimos4());
        var guardado = pagos.findAll().get(0);
        assertEquals(64, guardado.getTarjetaHash().length());
        assertFalse(guardado.getTarjetaHash().contains("4111"));
        verify(publicador).publicar(any());
    }

    @Test
    void rechazaUnNumeroDeTarjetaInvalido() {
        Long orden = ordenExistente("1000.00");

        PagoResponse r = service.procesar(USUARIO, TOKEN, pago(orden, "4000000000000001"));

        assertEquals(EstadoPago.RECHAZADO, r.estado());
        assertEquals("Numero de tarjeta invalido", r.motivo());
    }

    @Test
    void rechazaUnaTarjetaVencida() {
        Long orden = ordenExistente("1000.00");

        PagoResponse r = service.procesar(USUARIO, TOKEN,
                new PagoRequest(orden, "5555555555554444", "Ana", "01/20", "123"));

        assertEquals(EstadoPago.RECHAZADO, r.estado());
        assertEquals("Tarjeta vencida", r.motivo());
    }

    @Test
    void laTarjetaDePruebaTerminadaEnCeroCeroCeroDosNoTieneFondos() {
        Long orden = ordenExistente("1000.00");

        PagoResponse r = service.procesar(USUARIO, TOKEN, pago(orden, "4000000000180002"));

        assertEquals(EstadoPago.RECHAZADO, r.estado());
        assertEquals("Fondos insuficientes", r.motivo());
    }

    @Test
    void bloqueaAlUsuarioALaCuartaCompraEnUnMinutoYRegistraLaAlerta() {
        Long orden = ordenExistente("1000.00");
        String tarjeta = "4000000000000002"; // siempre rechazada: la orden sigue sin pagar

        for (int i = 0; i < 3; i++) {
            assertEquals(EstadoPago.RECHAZADO, service.procesar(USUARIO, TOKEN, pago(orden, tarjeta)).estado());
        }
        PagoResponse cuarto = service.procesar(USUARIO, TOKEN, pago(orden, tarjeta));

        assertEquals(EstadoPago.BLOQUEADO_FRAUDE, cuarto.estado());
        assertTrue(cuarto.motivo().contains("bloqueado"));
        var alertasGuardadas = alertas.findAll();
        assertEquals(1, alertasGuardadas.size());
        assertEquals("0002", alertasGuardadas.get(0).getTarjetaUltimos4());
        assertEquals(4, alertasGuardadas.get(0).getIntentos());
        assertEquals(60, alertasGuardadas.get(0).getVentanaSegundos());
        verify(publicador, times(4)).publicar(any()); // 3 rechazos + 1 fraude, todos hacia Kafka

        // Sigue bloqueado aunque use otra tarjeta: el bloqueo es del usuario (15 minutos).
        PagoResponse otraTarjeta = service.procesar(USUARIO, TOKEN, pago(ordenExistente("1000.00"), "5555555555554444"));
        assertEquals(EstadoPago.BLOQUEADO_FRAUDE, otraTarjeta.estado());
    }

    @Test
    void unaOrdenYaPagadaNoSePuedePagarDeNuevo() {
        Long orden = ordenExistente("500.00");
        assertEquals(EstadoPago.APROBADO, service.procesar(USUARIO, TOKEN, pago(orden, "5555555555554444")).estado());

        PagoException ex = assertThrows(PagoException.class,
                () -> service.procesar(USUARIO, TOKEN, pago(orden, "5555555555554444")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(1, pagos.count());
    }

    @Test
    void unaOrdenQueNoExisteONoEsDelUsuarioDaNotFoundYNoCuentaComoPago() {
        when(ordenes.buscar(eq(999L), any())).thenReturn(Optional.empty());

        PagoException ex = assertThrows(PagoException.class,
                () -> service.procesar(USUARIO, TOKEN, pago(999L, "378282246310005")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(0, pagos.count());
        verify(publicador, never()).publicar(any());
    }

    @Test
    void listaLosPagosDelUsuarioYLasAlertas() {
        Long orden = ordenExistente("500.00");
        service.procesar(USUARIO, TOKEN, pago(orden, "6011111111111117"));

        assertEquals(1, service.listar(USUARIO, null).size());
        assertEquals(1, service.listar(USUARIO, orden).size());
        assertEquals(0, service.listar("otro-usuario", null).size());
        assertNull(service.listar(USUARIO, null).get(0).motivo());
        assertEquals(0, service.alertasDeFraude().size());
    }
}

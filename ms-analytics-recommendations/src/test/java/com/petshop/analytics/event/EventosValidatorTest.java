package com.petshop.analytics.event;

import com.petshop.analytics.Eventos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventosValidatorTest {

    private final EventosValidator validador = new EventosValidator();

    @Test
    void aceptaUnEventoDeNavegacionValido() {
        assertDoesNotThrow(() -> validador.validar(Eventos.visto("u1", 5L, "Gatos")));
    }

    @Test
    void aceptaTiposSinProductoComoCuponesYEnvios() {
        assertDoesNotThrow(() -> validador.validar(Eventos.navegacion(NavegacionEvent.CUPON_APLICADO, "u1", null, null)));
        assertDoesNotThrow(() -> validador.validar(Eventos.navegacion(NavegacionEvent.ENVIO_GESTIONADO, "u1", null, null)));
    }

    @Test
    void rechazaLosEventosMalFormados() {
        assertThrows(EventoInvalidoException.class, () -> validador.validar((NavegacionEvent) null));
        assertThrows(EventoInvalidoException.class,
                () -> validador.validar(new NavegacionEvent(null, "PRODUCTO_VISTO", "u", 1L, null, null, null, null, null, Instant.now())));
        assertThrows(EventoInvalidoException.class,
                () -> validador.validar(new NavegacionEvent("e", "PRODUCTO_VISTO", " ", 1L, null, null, null, null, null, Instant.now())));
        assertThrows(EventoInvalidoException.class,
                () -> validador.validar(new NavegacionEvent("e", "TIPO_RARO", "u", 1L, null, null, null, null, null, Instant.now())));
        assertThrows(EventoInvalidoException.class,
                () -> validador.validar(new NavegacionEvent("e", "PRODUCTO_VISTO", "u", 1L, null, null, null, null, null, null)));
    }

    @Test
    void unEventoDeProductoDebeIndicarElProducto() {
        assertThrows(EventoInvalidoException.class,
                () -> validador.validar(Eventos.navegacion(NavegacionEvent.ITEM_AGREGADO, "u1", null, "Gatos")));
    }

    @Test
    void laCantidadDebeSerPositiva() {
        NavegacionEvent cero = new NavegacionEvent("e", "ITEM_AGREGADO", "u", 1L, null, null, BigDecimal.ONE, 0, null, Instant.now());
        assertThrows(EventoInvalidoException.class, () -> validador.validar(cero));
    }

    @Test
    void validaLosEventosDePago() {
        assertDoesNotThrow(() -> validador.validar(Eventos.pago(PagoEvent.PAGO_APROBADO, 1L)));
        assertDoesNotThrow(() -> validador.validar(Eventos.pago(PagoEvent.FRAUDE_BLOQUEADO, 1L)));
        assertThrows(EventoInvalidoException.class, () -> validador.validar(Eventos.pago("OTRO", 1L)));
        assertThrows(EventoInvalidoException.class, () -> validador.validar(Eventos.pago(PagoEvent.PAGO_APROBADO, null)));
        assertThrows(EventoInvalidoException.class, () -> validador.validar((PagoEvent) null));
    }
}

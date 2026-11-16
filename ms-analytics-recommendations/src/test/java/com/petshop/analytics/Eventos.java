package com.petshop.analytics;

import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.event.PagoEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Fabrica de eventos para las pruebas. */
public final class Eventos {

    private Eventos() {
    }

    public static NavegacionEvent navegacion(String tipo, String usuario, Long productoId, String categoria) {
        return navegacion(tipo, usuario, productoId, categoria, Instant.now());
    }

    public static NavegacionEvent navegacion(String tipo, String usuario, Long productoId, String categoria, Instant momento) {
        return new NavegacionEvent(UUID.randomUUID().toString(), tipo, usuario, productoId,
                productoId == null ? null : "Producto " + productoId, categoria, new BigDecimal("1000.00"), 1,
                "ms-test", momento);
    }

    public static NavegacionEvent visto(String usuario, Long productoId, String categoria) {
        return navegacion(NavegacionEvent.PRODUCTO_VISTO, usuario, productoId, categoria);
    }

    public static PagoEvent pago(String tipo, Long ordenId) {
        return new PagoEvent(UUID.randomUUID().toString(), tipo, ordenId, "usuario-pago", new BigDecimal("5000.00"),
                "1111", PagoEvent.PAGO_APROBADO.equals(tipo) ? null : "motivo de prueba", Instant.now());
    }
}

package com.petshop.analytics.event;

import org.springframework.stereotype.Component;

import java.util.Set;

/** Valida el contenido de los eventos antes de procesarlos: un mensaje defectuoso no debe contaminar las metricas. */
@Component
public class EventosValidator {

    private static final Set<String> TIPOS_NAVEGACION = Set.of(
            NavegacionEvent.PRODUCTO_VISTO, NavegacionEvent.ITEM_AGREGADO, NavegacionEvent.ITEM_ELIMINADO,
            NavegacionEvent.COMPRA_REALIZADA, NavegacionEvent.CUPON_APLICADO, NavegacionEvent.ENVIO_GESTIONADO);

    /** Tipos que siempre se refieren a un producto concreto. */
    private static final Set<String> TIPOS_CON_PRODUCTO = Set.of(
            NavegacionEvent.PRODUCTO_VISTO, NavegacionEvent.ITEM_AGREGADO, NavegacionEvent.ITEM_ELIMINADO,
            NavegacionEvent.COMPRA_REALIZADA);

    private static final Set<String> TIPOS_PAGO = Set.of(
            PagoEvent.PAGO_APROBADO, PagoEvent.PAGO_RECHAZADO, PagoEvent.FRAUDE_BLOQUEADO);

    public void validar(NavegacionEvent e) {
        if (e == null) {
            throw new EventoInvalidoException("mensaje vacio");
        }
        requerido(e.eventId(), "eventId");
        requerido(e.usuarioId(), "usuarioId");
        requerido(e.tipo(), "tipo");
        if (!TIPOS_NAVEGACION.contains(e.tipo())) {
            throw new EventoInvalidoException("tipo de navegacion desconocido: " + e.tipo());
        }
        if (e.timestamp() == null) {
            throw new EventoInvalidoException("falta timestamp");
        }
        if (TIPOS_CON_PRODUCTO.contains(e.tipo()) && e.productoId() == null) {
            throw new EventoInvalidoException(e.tipo() + " requiere productoId");
        }
        if (e.cantidad() != null && e.cantidad() <= 0) {
            throw new EventoInvalidoException("cantidad debe ser positiva: " + e.cantidad());
        }
    }

    public void validar(PagoEvent e) {
        if (e == null) {
            throw new EventoInvalidoException("mensaje vacio");
        }
        requerido(e.eventId(), "eventId");
        requerido(e.usuarioId(), "usuarioId");
        requerido(e.tipo(), "tipo");
        if (!TIPOS_PAGO.contains(e.tipo())) {
            throw new EventoInvalidoException("tipo de pago desconocido: " + e.tipo());
        }
        if (e.ordenId() == null) {
            throw new EventoInvalidoException("falta ordenId");
        }
        if (e.timestamp() == null) {
            throw new EventoInvalidoException("falta timestamp");
        }
    }

    private static void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new EventoInvalidoException("falta " + campo);
        }
    }
}

package com.petshop.pago.antifraude;

import java.time.Instant;

/**
 * Evento del topico pagos.intentos: un intento de pago (compra) de un usuario.
 * La clave del mensaje es el usuario: todos sus intentos van a la misma particion, en orden.
 */
public record IntentoCompraEvent(String usuarioId, Long ordenId, Instant timestamp) {
}

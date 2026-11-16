package com.petshop.productos.messaging.event;

/**
 * Evento stock.bajo: lo publica ms-productos (exchange topic) cuando un descuento deja un
 * producto con stock igual o menor al umbral. ms-notificaciones avisa por correo.
 */
public record StockBajoEvent(Long productoId, String nombre, int stock, int umbral) {
}

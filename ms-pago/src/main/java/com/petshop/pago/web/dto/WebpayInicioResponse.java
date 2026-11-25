package com.petshop.pago.web.dto;

/** A donde enviar al usuario: formulario POST a url con el campo token_ws = token. */
public record WebpayInicioResponse(String url, String token) {
}

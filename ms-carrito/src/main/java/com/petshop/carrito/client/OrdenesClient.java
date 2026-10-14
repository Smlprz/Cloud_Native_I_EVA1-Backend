package com.petshop.carrito.client;

import com.petshop.carrito.web.dto.PedidoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente HTTP hacia ms-orden. Reenvia el JWT del usuario. Es "best effort":
 * si ms-orden no esta configurado o falla, la compra NO se ve afectada.
 * ms-orden publica el evento en RabbitMQ y ms-notificaciones envia el correo.
 */
@Component
public class OrdenesClient {

    private static final Logger log = LoggerFactory.getLogger(OrdenesClient.class);

    private final RestClient restClient;

    public OrdenesClient(@Value("${services.ordenes.base-url:}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** email: opcional. Si viene, ms-orden envia el comprobante a esa direccion. */
    public void registrarOrden(String bearerToken, PedidoResponse pedido, String email) {
        if (restClient == null) {
            return;
        }
        try {
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("total", pedido.total());
            if (email != null && !email.isBlank()) {
                cuerpo.put("email", email.trim());
            }
            restClient.post()
                    .uri("/api/ordenes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("No se pudo registrar la orden del pedido {} en ms-orden: {}", pedido.id(), e.getMessage());
        }
    }
}

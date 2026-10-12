package com.petshop.carrito.client;

import com.petshop.carrito.web.dto.PedidoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.stream.Collectors;

/**
 * Cliente HTTP hacia ms-notificaciones. El envio del correo es "best effort":
 * si el servicio no esta configurado o falla, la compra NO se ve afectada.
 */
@Component
public class NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClient.class);

    private final RestClient restClient;

    public NotificacionesClient(@Value("${services.notificaciones.base-url:}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void enviarConfirmacionPedido(String email, PedidoResponse pedido) {
        if (restClient == null || email == null || email.isBlank()) {
            return;
        }
        try {
            String detalle = pedido.items().stream()
                    .map(i -> "- " + i.nombreProducto() + " x" + i.cantidad() + " = $" + i.subtotal())
                    .collect(Collectors.joining("\n"));
            String cuerpo = "Gracias por tu compra en Pet Shop.\n\n"
                    + "Pedido N° " + pedido.id() + "\n" + detalle + "\n\nTotal: $" + pedido.total();

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("to", email);
            form.add("subject", "Confirmacion de tu pedido N° " + pedido.id());
            form.add("body", cuerpo);

            restClient.post().uri("/api/email/enviar").body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de confirmacion del pedido {}: {}", pedido.id(), e.getMessage());
        }
    }
}

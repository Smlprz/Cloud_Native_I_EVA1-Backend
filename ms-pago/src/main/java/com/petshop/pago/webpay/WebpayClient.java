package com.petshop.pago.webpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST de Webpay Plus (Transbank). Por defecto apunta al ambiente de INTEGRACION con las
 * credenciales publicas de prueba que publica Transbank; para produccion se cambian por variables de entorno.
 */
@Component
public class WebpayClient {

    private static final String RUTA = "/rswebpaytransaction/api/webpay/v1.2/transactions";

    private final RestClient restClient;

    public WebpayClient(@Value("${app.webpay.base-url}") String baseUrl,
                        @Value("${app.webpay.commerce-code}") String commerceCode,
                        @Value("${app.webpay.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Tbk-Api-Key-Id", commerceCode)
                .defaultHeader("Tbk-Api-Key-Secret", apiKey)
                .build();
    }

    /** Crea la transaccion: devuelve el token y la URL del formulario de pago de Transbank. */
    public Creada crear(String buyOrder, String sessionId, long monto, String returnUrl) {
        try {
            return restClient.post().uri(RUTA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Solicitud(buyOrder, sessionId, monto, returnUrl))
                    .retrieve()
                    .body(Creada.class);
        } catch (RestClientException e) {
            throw new WebpayNoDisponibleException("No se pudo crear la transaccion en Webpay: " + e.getMessage(), e);
        }
    }

    /** Confirma (commit) la transaccion cuando el usuario vuelve desde Webpay. */
    public Confirmacion confirmar(String token) {
        try {
            return restClient.put().uri(RUTA + "/{token}", token)
                    .retrieve()
                    .body(Confirmacion.class);
        } catch (RestClientException e) {
            throw new WebpayNoDisponibleException("No se pudo confirmar la transaccion en Webpay: " + e.getMessage(), e);
        }
    }

    record Solicitud(@JsonProperty("buy_order") String buyOrder,
                     @JsonProperty("session_id") String sessionId,
                     long amount,
                     @JsonProperty("return_url") String returnUrl) {
    }

    public record Creada(String token, String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Confirmacion(String status,
                               @JsonProperty("response_code") Integer responseCode,
                               @JsonProperty("buy_order") String buyOrder,
                               long amount,
                               @JsonProperty("card_detail") Tarjeta cardDetail) {

        public boolean autorizada() {
            return "AUTHORIZED".equals(status) && responseCode != null && responseCode == 0;
        }

        /** Webpay solo entrega los ultimos 4 digitos: el comercio nunca ve la tarjeta completa. */
        public String ultimos4() {
            String n = cardDetail == null ? null : cardDetail.cardNumber();
            return n == null || n.length() < 4 ? "WPAY" : n.substring(n.length() - 4);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tarjeta(@JsonProperty("card_number") String cardNumber) {
    }
}

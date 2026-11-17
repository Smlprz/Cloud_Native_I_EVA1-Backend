package com.petshop.carrito.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.carrito.service.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Cliente HTTP hacia ms-cupones. Reenvia el JWT del usuario: el cupon queda ligado a quien compra.
 * Si el cliente envia un cupon y no se puede aplicar, el checkout se rechaza con el motivo (nunca se cobra de mas
 * ni se ignora el cupon en silencio).
 */
@Component
public class CuponesClient {

    private static final Logger log = LoggerFactory.getLogger(CuponesClient.class);

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public CuponesClient(RestClient.Builder builder, @Value("${services.cupones.base-url:}") String baseUrl) {
        this.restClient = (baseUrl == null || baseUrl.isBlank()) ? null : builder.baseUrl(baseUrl).build();
    }

    /** Consume el cupon sobre el monto indicado y devuelve el descuento. */
    public CuponAplicado aplicar(String bearerToken, String codigo, BigDecimal monto) {
        if (restClient == null) {
            throw new ReglaNegocioException("Los cupones no estan disponibles por ahora");
        }
        try {
            CuponAplicado respuesta = restClient.post()
                    .uri("/api/cupones/aplicar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(Map.of("codigo", codigo, "monto", monto))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), (req, res) -> {
                        throw new ReglaNegocioException(motivo(res.getBody()));
                    })
                    .body(CuponAplicado.class);
            if (respuesta == null || respuesta.descuento() == null) {
                throw new ReglaNegocioException("No se pudo aplicar el cupon");
            }
            return respuesta;
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("ms-cupones no respondio al aplicar el cupon {}: {}", codigo, e.getMessage());
            throw new ReglaNegocioException("No se pudo validar el cupon en este momento. Intenta de nuevo o compra sin cupon");
        }
    }

    /** El motivo viene en el campo "detail" del error (RFC 7807) de ms-cupones. */
    private String motivo(java.io.InputStream cuerpo) {
        try {
            JsonNode json = mapper.readTree(cuerpo);
            if (json != null && json.hasNonNull("detail")) {
                return json.get("detail").asText();
            }
        } catch (IOException e) {
            // cuerpo sin JSON: mensaje generico
        }
        return "El cupon no se pudo aplicar";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CuponAplicado(String codigo, BigDecimal descuento) {
    }
}

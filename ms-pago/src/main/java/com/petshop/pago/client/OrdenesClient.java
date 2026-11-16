package com.petshop.pago.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cliente HTTP hacia ms-orden. El monto de un pago sale de la orden (no de lo que mande el cliente) y solo se
 * puede pagar una orden propia: se reenvia el JWT del usuario y ms-orden lista unicamente SUS ordenes.
 */
@Component
public class OrdenesClient {

    private final RestClient restClient;

    public OrdenesClient(@Value("${services.ordenes.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Busca una orden del usuario del token. Vacio si no existe o no es suya. */
    public Optional<OrdenDto> buscar(Long ordenId, String bearerToken) {
        try {
            OrdenDto[] ordenes = restClient.get()
                    .uri("/api/ordenes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(OrdenDto[].class);
            return ordenes == null ? Optional.empty()
                    : Arrays.stream(ordenes).filter(o -> ordenId.equals(o.id())).findFirst();
        } catch (RestClientException e) {
            throw new OrdenesNoDisponibleException("No se pudo consultar ms-orden: " + e.getMessage(), e);
        }
    }
}

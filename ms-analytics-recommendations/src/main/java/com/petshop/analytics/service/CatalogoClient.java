package com.petshop.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Catalogo de productos para poder recomendar. Lo descarga de ms-productos (el catalogo es publico, no pide token)
 * y lo reutiliza durante unos segundos. Si ms-productos no responde se sigue con la ultima copia que se tenia.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);
    /** Si falla la descarga se reintenta pronto, pero no en cada peticion. */
    private static final long REINTENTO_TRAS_ERROR_MS = 5_000;

    private final RestClient restClient;
    private final long ttlMs;

    private volatile List<ProductoCatalogo> copia = List.of();
    private volatile long proximaCargaMs = 0;

    public CatalogoClient(@Value("${services.productos.base-url}") String baseUrl,
                          @Value("${analytics.recomendaciones.catalogo-ttl-segundos:60}") long ttlSegundos) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.ttlMs = ttlSegundos * 1000;
    }

    public List<ProductoCatalogo> catalogo() {
        if (System.currentTimeMillis() >= proximaCargaMs) {
            refrescar();
        }
        return copia;
    }

    public Optional<ProductoCatalogo> buscar(Long productoId) {
        return catalogo().stream().filter(p -> p.id().equals(productoId)).findFirst();
    }

    private synchronized void refrescar() {
        if (System.currentTimeMillis() < proximaCargaMs) {
            return; // otro hilo ya lo refresco
        }
        try {
            ProductoCatalogo[] productos = restClient.get().uri("/api/productos").retrieve().body(ProductoCatalogo[].class);
            copia = productos == null ? List.of() : List.copyOf(Arrays.asList(productos));
            proximaCargaMs = System.currentTimeMillis() + ttlMs;
            log.debug("Catalogo actualizado: {} productos", copia.size());
        } catch (Exception e) {
            proximaCargaMs = System.currentTimeMillis() + REINTENTO_TRAS_ERROR_MS;
            log.warn("No se pudo descargar el catalogo de ms-productos ({}). Se usa la ultima copia ({} productos).",
                    e.getMessage(), copia.size());
        }
    }
}

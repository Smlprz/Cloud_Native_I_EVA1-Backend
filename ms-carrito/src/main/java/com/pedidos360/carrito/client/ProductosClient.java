package com.pedidos360.carrito.client;

import com.pedidos360.carrito.service.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el microservicio de Productos.
 * Reenvia el mismo token JWT del usuario para que la llamada entre servicios
 * viaje autenticada de extremo a extremo.
 */
@Component
public class ProductosClient {

    private final RestClient restClient;

    public ProductosClient(RestClient.Builder builder,
                           @Value("${services.productos.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ProductoDto obtenerProducto(Long productoId, String bearerToken) {
        ProductoDto producto = restClient.get()
                .uri("/api/productos/{id}", productoId)
                .headers(headers -> {
                    if (bearerToken != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
                    }
                })
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new ReglaNegocioException("El producto " + productoId + " no existe");
                })
                .body(ProductoDto.class);

        if (producto == null) {
            throw new ReglaNegocioException("No se pudo obtener el producto " + productoId);
        }
        return producto;
    }
}

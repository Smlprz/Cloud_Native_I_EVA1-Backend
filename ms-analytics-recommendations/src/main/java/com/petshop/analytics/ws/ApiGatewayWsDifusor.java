package com.petshop.analytics.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dashboards conectados por el API Gateway WebSocket (pedidos360-ws). API Gateway mantiene la conexion con el
 * navegador; este servicio solo guarda el connectionId y le envia las metricas con POST @connections/{id}
 * (credenciales del rol de la EC2). Sin WS_GATEWAY_ENDPOINT queda desactivado.
 */
@Component
public class ApiGatewayWsDifusor {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayWsDifusor.class);

    private final Set<String> conexiones = ConcurrentHashMap.newKeySet();
    private final ApiGatewayManagementApiClient cliente;
    private volatile boolean hayNuevas;

    public ApiGatewayWsDifusor(@Value("${analytics.ws.gateway-endpoint:}") String endpoint,
                               @Value("${analytics.dynamodb.region:us-east-1}") String region) {
        this.cliente = endpoint == null || endpoint.isBlank() ? null : ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    public boolean habilitado() {
        return cliente != null;
    }

    public void conectar(String connectionId) {
        conexiones.add(connectionId);
        hayNuevas = true;
        log.info("Dashboard conectado por API Gateway WebSocket ({} conexion(es))", conexiones.size());
    }

    public void desconectar(String connectionId) {
        if (conexiones.remove(connectionId)) {
            log.info("Dashboard desconectado de API Gateway WebSocket ({} conexion(es))", conexiones.size());
        }
    }

    public int conectados() {
        return conexiones.size();
    }

    /** true una sola vez despues de cada conexion nueva: para enviarle la foto actual de inmediato. */
    public boolean tomarNuevas() {
        boolean nuevas = hayNuevas;
        hayNuevas = false;
        return nuevas;
    }

    public void difundir(String mensaje) {
        if (cliente == null || conexiones.isEmpty()) {
            return;
        }
        SdkBytes datos = SdkBytes.fromUtf8String(mensaje);
        for (String id : conexiones) {
            try {
                cliente.postToConnection(r -> r.connectionId(id).data(datos));
            } catch (GoneException e) {
                conexiones.remove(id); // el navegador ya se fue
            } catch (Exception e) {
                log.warn("No se pudo enviar al dashboard {}: {}", id, e.getMessage());
            }
        }
    }
}

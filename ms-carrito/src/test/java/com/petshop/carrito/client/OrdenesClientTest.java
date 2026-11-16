package com.petshop.carrito.client;

import com.petshop.carrito.web.dto.PedidoResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica el cuerpo que ms-carrito envia a ms-orden: total, email e items para el descuento de stock. */
class OrdenesClientTest {

    private HttpServer server;
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/ordenes", exchange -> {
            cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacion.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void detenerServidor() {
        server.stop(0);
    }

    private OrdenesClient cliente() {
        return new OrdenesClient("http://localhost:" + server.getAddress().getPort());
    }

    @Test
    void enviaTotalEmailYLosItemsDelPedido() {
        PedidoResponse pedido = new PedidoResponse(9L, new BigDecimal("7980"), "CONFIRMADO", LocalDateTime.now(),
                List.of(new PedidoResponse.ItemResponse(12L, "Collar", new BigDecimal("3990"), 2, new BigDecimal("7980"))));

        cliente().registrarOrden("token-x", pedido, "cliente@correo.cl");

        assertNotNull(cuerpo.get());
        assertTrue(cuerpo.get().contains("\"total\":7980"), cuerpo.get());
        assertTrue(cuerpo.get().contains("\"email\":\"cliente@correo.cl\""), cuerpo.get());
        assertTrue(cuerpo.get().contains("\"productoId\":12"), cuerpo.get());
        assertTrue(cuerpo.get().contains("\"cantidad\":2"), cuerpo.get());
        assertEquals("Bearer token-x", autorizacion.get());
    }

    @Test
    void sinItemsNoEnviaLaClaveItems() {
        PedidoResponse pedido = new PedidoResponse(10L, new BigDecimal("500"), "CONFIRMADO", LocalDateTime.now(), List.of());

        cliente().registrarOrden("token-y", pedido, null);

        assertNotNull(cuerpo.get());
        assertTrue(!cuerpo.get().contains("items"), cuerpo.get());
    }
}

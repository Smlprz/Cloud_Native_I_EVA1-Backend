package com.petshop.analytics.store;

import com.petshop.analytics.Eventos;
import com.petshop.analytics.event.NavegacionEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba contra DynamoDB de verdad. Solo corre si se define DYNAMODB_ENDPOINT (por ejemplo DynamoDB Local en
 * http://localhost:8000); sin esa variable se omite, para que "mvn test" no dependa de ningun servicio externo.
 */
@EnabledIfEnvironmentVariable(named = "DYNAMODB_ENDPOINT", matches = ".+")
class DynamoAnalyticsStoreIT {

    private static DynamoDbClient cliente;
    private static DynamoAnalyticsStore store;
    private static final String SUFIJO = UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void conectar() {
        cliente = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(UrlConnectionHttpClient.create())
                .endpointOverride(URI.create(System.getenv("DYNAMODB_ENDPOINT")))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                .build();
        store = new DynamoAnalyticsStore(cliente, "it_eventos_" + SUFIJO, "it_perfiles_" + SUFIJO, true);
        store.prepararTablas();
    }

    @AfterAll
    static void limpiar() {
        for (String tabla : List.of("it_eventos_" + SUFIJO, "it_perfiles_" + SUFIJO)) {
            cliente.deleteTable(b -> b.tableName(tabla));
        }
        cliente.close();
    }

    @Test
    void guardaElHistorialSinDuplicadosYLoDevuelveDelMasRecienteAlMasAntiguo() {
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        NavegacionEvent primero = Eventos.navegacion("PRODUCTO_VISTO", "u-dynamo", 1L, "Gatos", t);
        NavegacionEvent segundo = Eventos.navegacion("ITEM_AGREGADO", "u-dynamo", 2L, "Perros", t.plusSeconds(5));

        assertTrue(store.guardarEvento(primero));
        assertFalse(store.guardarEvento(primero), "el mismo evento no se guarda dos veces");
        assertTrue(store.guardarEvento(segundo));

        List<NavegacionEvent> ultimos = store.ultimosEventos("u-dynamo", 10);
        assertEquals(2, ultimos.size());
        assertEquals(2L, ultimos.get(0).productoId());
        assertEquals("ITEM_AGREGADO", ultimos.get(0).tipo());
        assertEquals("Gatos", ultimos.get(1).categoria());
        assertEquals(t, ultimos.get(1).timestamp());
    }

    @Test
    void guardaYRecuperaElPerfilDeNavegacion() {
        PerfilUsuario perfil = new PerfilUsuario("u-perfil");
        perfil.sumarCategoria("Gatos", 6);
        perfil.sumarCategoria("Perros", 1.5);
        perfil.registrarVisto(1L);
        perfil.registrarVisto(2L);
        perfil.registrarCompra(9L);
        store.guardarPerfil(perfil);

        PerfilUsuario leido = store.obtenerPerfil("u-perfil");

        assertEquals(6.0, leido.getCategorias().get("Gatos"));
        assertEquals(1.5, leido.getCategorias().get("Perros"));
        assertEquals(List.of(2L, 1L), leido.getVistosRecientes()); // el mas reciente primero
        assertTrue(leido.getComprados().contains(9L));
    }

    @Test
    void unUsuarioSinPerfilDevuelveUnoVacio() {
        assertTrue(store.obtenerPerfil("nadie-" + SUFIJO).getCategorias().isEmpty());
    }
}

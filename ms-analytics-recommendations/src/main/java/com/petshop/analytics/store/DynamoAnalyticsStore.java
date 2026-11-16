package com.petshop.analytics.store;

import com.petshop.analytics.event.NavegacionEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Almacen en Amazon DynamoDB (analytics.store=dynamodb).
 *
 *   pedidos360_eventos    historial de eventos      PK usuarioId (S)   SK eventKey (S) = "<instante>#<eventId>"
 *   pedidos360_perfiles   perfil de navegacion      PK usuarioId (S)
 *
 * Las tablas se crean al arrancar si no existen (modo on-demand: sin capacidad que dimensionar).
 * El PutItem del historial es condicional (attribute_not_exists): un mensaje duplicado de Kafka no se guarda dos veces.
 */
@Component
@ConditionalOnProperty(name = "analytics.store", havingValue = "dynamodb")
public class DynamoAnalyticsStore implements AnalyticsStore {

    private static final Logger log = LoggerFactory.getLogger(DynamoAnalyticsStore.class);

    private final DynamoDbClient dynamo;
    private final String tablaEventos;
    private final String tablaPerfiles;
    private final boolean crearTablas;

    public DynamoAnalyticsStore(DynamoDbClient dynamo,
                                @Value("${analytics.dynamodb.tabla-eventos}") String tablaEventos,
                                @Value("${analytics.dynamodb.tabla-perfiles}") String tablaPerfiles,
                                @Value("${analytics.dynamodb.crear-tablas:true}") boolean crearTablas) {
        this.dynamo = dynamo;
        this.tablaEventos = tablaEventos;
        this.tablaPerfiles = tablaPerfiles;
        this.crearTablas = crearTablas;
    }

    @PostConstruct
    void prepararTablas() {
        if (!crearTablas) {
            return;
        }
        asegurarTabla(tablaEventos, "usuarioId", "eventKey");
        asegurarTabla(tablaPerfiles, "usuarioId", null);
    }

    @Override
    public boolean guardarEvento(NavegacionEvent e) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("usuarioId", s(e.usuarioId()));
        item.put("eventKey", s(e.timestamp() + "#" + e.eventId()));
        item.put("eventId", s(e.eventId()));
        item.put("tipo", s(e.tipo()));
        item.put("timestamp", s(e.timestamp().toString()));
        putSiHay(item, "productoId", e.productoId());
        putSiHay(item, "nombreProducto", e.nombreProducto());
        putSiHay(item, "categoria", e.categoria());
        putSiHay(item, "precio", e.precio());
        putSiHay(item, "cantidad", e.cantidad());
        putSiHay(item, "origen", e.origen());
        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(tablaEventos)
                    .item(item)
                    .conditionExpression("attribute_not_exists(eventKey)")
                    .build());
            return true;
        } catch (ConditionalCheckFailedException duplicado) {
            return false;
        }
    }

    @Override
    public List<NavegacionEvent> ultimosEventos(String usuarioId, int limite) {
        var respuesta = dynamo.query(QueryRequest.builder()
                .tableName(tablaEventos)
                .keyConditionExpression("usuarioId = :u")
                .expressionAttributeValues(Map.of(":u", s(usuarioId)))
                .scanIndexForward(false)
                .limit(limite)
                .build());
        List<NavegacionEvent> lista = new ArrayList<>();
        for (Map<String, AttributeValue> item : respuesta.items()) {
            lista.add(new NavegacionEvent(
                    texto(item, "eventId"), texto(item, "tipo"), texto(item, "usuarioId"),
                    numeroLong(item, "productoId"), texto(item, "nombreProducto"), texto(item, "categoria"),
                    item.containsKey("precio") ? new BigDecimal(item.get("precio").n()) : null,
                    item.containsKey("cantidad") ? Integer.valueOf(item.get("cantidad").n()) : null,
                    texto(item, "origen"), Instant.parse(texto(item, "timestamp"))));
        }
        return lista;
    }

    @Override
    public PerfilUsuario obtenerPerfil(String usuarioId) {
        var respuesta = dynamo.getItem(GetItemRequest.builder()
                .tableName(tablaPerfiles)
                .key(Map.of("usuarioId", s(usuarioId)))
                .consistentRead(true)
                .build());
        PerfilUsuario perfil = new PerfilUsuario(usuarioId);
        if (!respuesta.hasItem() || respuesta.item().isEmpty()) {
            return perfil;
        }
        Map<String, AttributeValue> item = respuesta.item();
        if (item.containsKey("categorias")) {
            item.get("categorias").m().forEach((cat, v) -> perfil.sumarCategoria(cat, Double.parseDouble(v.n())));
        }
        if (item.containsKey("vistos")) {
            List<AttributeValue> vistos = new ArrayList<>(item.get("vistos").l());
            java.util.Collections.reverse(vistos); // se guardan del mas reciente al mas antiguo
            vistos.forEach(v -> perfil.registrarVisto(Long.parseLong(v.n())));
        }
        if (item.containsKey("comprados")) {
            item.get("comprados").l().forEach(v -> perfil.registrarCompra(Long.parseLong(v.n())));
        }
        if (item.containsKey("actualizado")) {
            perfil.marcarActualizado(Instant.parse(item.get("actualizado").s()));
        }
        return perfil;
    }

    @Override
    public void guardarPerfil(PerfilUsuario perfil) {
        Map<String, AttributeValue> categorias = new HashMap<>();
        perfil.getCategorias().forEach((cat, puntos) -> categorias.put(cat, n(puntos)));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("usuarioId", s(perfil.getUsuarioId()));
        item.put("categorias", AttributeValue.builder().m(categorias).build());
        item.put("vistos", AttributeValue.builder()
                .l(perfil.getVistosRecientes().stream().map(DynamoAnalyticsStore::n).toList()).build());
        item.put("comprados", AttributeValue.builder()
                .l(perfil.getComprados().stream().map(DynamoAnalyticsStore::n).toList()).build());
        item.put("actualizado", s(perfil.getActualizado().toString()));
        dynamo.putItem(PutItemRequest.builder().tableName(tablaPerfiles).item(item).build());
    }

    @Override
    public String nombre() {
        return "DynamoDB (" + tablaEventos + ", " + tablaPerfiles + ")";
    }

    // ------------------------------------------------------------------ apoyo

    private void asegurarTabla(String tabla, String clavePrimaria, String claveOrden) {
        try {
            dynamo.describeTable(DescribeTableRequest.builder().tableName(tabla).build());
            log.info("Tabla DynamoDB existente: {}", tabla);
            return;
        } catch (ResourceNotFoundException noExiste) {
            log.info("Creando la tabla DynamoDB {}...", tabla);
        }
        List<AttributeDefinition> atributos = new ArrayList<>();
        List<KeySchemaElement> claves = new ArrayList<>();
        atributos.add(AttributeDefinition.builder().attributeName(clavePrimaria).attributeType(ScalarAttributeType.S).build());
        claves.add(KeySchemaElement.builder().attributeName(clavePrimaria).keyType(KeyType.HASH).build());
        if (claveOrden != null) {
            atributos.add(AttributeDefinition.builder().attributeName(claveOrden).attributeType(ScalarAttributeType.S).build());
            claves.add(KeySchemaElement.builder().attributeName(claveOrden).keyType(KeyType.RANGE).build());
        }
        dynamo.createTable(CreateTableRequest.builder()
                .tableName(tabla)
                .attributeDefinitions(atributos)
                .keySchema(claves)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
        WaiterResponse<DescribeTableResponse> espera = dynamo.waiter()
                .waitUntilTableExists(DescribeTableRequest.builder().tableName(tabla).build());
        espera.matched().response().ifPresent(r -> log.info("Tabla DynamoDB lista: {} ({})", tabla, r.table().tableStatus()));
    }

    private static AttributeValue s(String valor) {
        return AttributeValue.builder().s(valor).build();
    }

    private static AttributeValue n(Number valor) {
        return AttributeValue.builder().n(valor.toString()).build();
    }

    private static void putSiHay(Map<String, AttributeValue> item, String nombre, Object valor) {
        if (valor == null) {
            return;
        }
        item.put(nombre, valor instanceof Number n ? n(n) : s(valor.toString()));
    }

    private static String texto(Map<String, AttributeValue> item, String nombre) {
        AttributeValue v = item.get(nombre);
        return v == null ? null : v.s();
    }

    private static Long numeroLong(Map<String, AttributeValue> item, String nombre) {
        AttributeValue v = item.get(nombre);
        return v == null ? null : Long.valueOf(v.n());
    }
}

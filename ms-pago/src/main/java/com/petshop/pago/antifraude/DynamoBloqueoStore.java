package com.petshop.pago.antifraude;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Bloqueos en Amazon DynamoDB (app.antifraude.store=dynamodb). Tabla pedidos360-bloqueos:
 * clave usuarioId; "expira" (segundos epoch) es el atributo TTL, asi DynamoDB borra solo los bloqueos vencidos.
 * En AWS usa el rol de la EC2 (LabInstanceProfile). Con DYNAMODB_ENDPOINT apunta a DynamoDB Local.
 * La tabla se crea al arrancar si no existe.
 */
@Component
@ConditionalOnProperty(name = "app.antifraude.store", havingValue = "dynamodb")
public class DynamoBloqueoStore implements BloqueoStore {

    private static final Logger log = LoggerFactory.getLogger(DynamoBloqueoStore.class);
    private static final String CLAVE = "usuarioId";
    private static final String EXPIRA = "expira";

    private final DynamoDbClient dynamo;
    private final String tabla;

    public DynamoBloqueoStore(@Value("${app.antifraude.dynamodb.region}") String region,
                              @Value("${app.antifraude.dynamodb.endpoint:}") String endpoint,
                              @Value("${app.antifraude.dynamodb.tabla}") String tabla) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        this.dynamo = builder.build();
        this.tabla = tabla;
        crearTablaSiNoExiste();
    }

    @Override
    public Optional<Instant> bloqueadoHasta(String usuarioId, Instant ahora) {
        Map<String, AttributeValue> item = dynamo.getItem(r -> r.tableName(tabla)
                .key(Map.of(CLAVE, AttributeValue.fromS(usuarioId))).consistentRead(true)).item();
        if (item == null || !item.containsKey(EXPIRA)) {
            return Optional.empty();
        }
        // El TTL de DynamoDB borra con retraso: un item vencido se ignora.
        Instant hasta = Instant.ofEpochSecond(Long.parseLong(item.get(EXPIRA).n()));
        return hasta.isAfter(ahora) ? Optional.of(hasta) : Optional.empty();
    }

    @Override
    public void bloquear(String usuarioId, Instant hasta) {
        dynamo.putItem(r -> r.tableName(tabla).item(Map.of(
                CLAVE, AttributeValue.fromS(usuarioId),
                EXPIRA, AttributeValue.fromN(Long.toString(hasta.getEpochSecond())),
                "bloqueadoEn", AttributeValue.fromS(Instant.now().toString()))));
    }

    @Override
    public void desbloquear(String usuarioId) {
        dynamo.deleteItem(r -> r.tableName(tabla).key(Map.of(CLAVE, AttributeValue.fromS(usuarioId))));
    }

    private void crearTablaSiNoExiste() {
        try {
            dynamo.describeTable(r -> r.tableName(tabla));
            return;
        } catch (ResourceNotFoundException e) {
            log.info("Creando la tabla DynamoDB {}", tabla);
        } catch (Exception e) {
            log.error("No se pudo consultar la tabla DynamoDB {}: {}", tabla, e.getMessage());
            return;
        }
        try {
            dynamo.createTable(r -> r.tableName(tabla)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(CLAVE).attributeType(ScalarAttributeType.S).build())
                    .keySchema(KeySchemaElement.builder().attributeName(CLAVE).keyType(KeyType.HASH).build()));
            dynamo.waiter().waitUntilTableExists(r -> r.tableName(tabla));
            dynamo.updateTimeToLive(r -> r.tableName(tabla)
                    .timeToLiveSpecification(s -> s.attributeName(EXPIRA).enabled(true)));
        } catch (ResourceInUseException e) {
            log.info("La tabla {} ya la creo otra instancia", tabla);
        } catch (Exception e) {
            log.error("No se pudo crear la tabla DynamoDB {}: {}", tabla, e.getMessage());
        }
    }

    @PreDestroy
    void cerrar() {
        dynamo.close();
    }
}

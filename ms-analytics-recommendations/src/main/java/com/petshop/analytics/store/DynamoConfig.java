package com.petshop.analytics.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * Cliente de Amazon DynamoDB (analytics.store=dynamodb). En AWS usa el rol de la instancia EC2 (cadena de
 * credenciales por defecto). Con DYNAMODB_ENDPOINT definido apunta a DynamoDB Local para desarrollo.
 */
@Configuration
@ConditionalOnProperty(name = "analytics.store", havingValue = "dynamodb")
public class DynamoConfig {

    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDbClient(@Value("${analytics.dynamodb.region}") String region,
                                         @Value("${analytics.dynamodb.endpoint:}") String endpoint) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).credentialsProvider(credencialesLocales());
        }
        return builder.build();
    }

    /** DynamoDB Local acepta cualquier credencial; se usa la del entorno si existe y una ficticia si no. */
    private static AwsCredentialsProvider credencialesLocales() {
        if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"));
    }
}

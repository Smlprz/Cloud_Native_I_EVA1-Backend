package com.petshop.orden.messaging.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ que este servicio posee. ms-orden solo PUBLICA (orden.creada y
 * stock.descontar) en el exchange direct, asi que solo declara ese exchange; las colas y sus DLQ
 * las declara cada consumidor (ms-notificaciones y ms-productos). Los nombres vienen de
 * {@link MessagingProperties}.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitTopologyConfig {

    private final MessagingProperties props;

    public RabbitTopologyConfig(MessagingProperties props) {
        this.props = props;
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(props.exchanges().direct());
    }
}

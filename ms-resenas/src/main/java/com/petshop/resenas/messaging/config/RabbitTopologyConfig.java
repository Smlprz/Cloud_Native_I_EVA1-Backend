package com.petshop.resenas.messaging.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ms-resenas solo PUBLICA resena.creada en el exchange direct; la cola y su DLQ las declara
 * el consumidor (ms-productos).
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

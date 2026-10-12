package com.petshop.orden.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Productor: solo declara el exchange al que publica. La cola orden.creada y su DLQ
 * las declara el consumidor (ms-notificaciones).
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String ROUTING_KEY = "orden.creada";

    @Bean
    DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

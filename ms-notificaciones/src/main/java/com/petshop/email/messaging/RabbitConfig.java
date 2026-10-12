package com.petshop.email.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ del flujo de ordenes:
 *
 *   pedidos.exchange --(orden.creada)--> cola orden.creada --(falla)--> pedidos.dlx --> cola orden.creada.dlq
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String DLX = "pedidos.dlx";
    public static final String QUEUE = "orden.creada";
    public static final String DLQ = "orden.creada.dlq";
    public static final String ROUTING_KEY = "orden.creada";

    @Bean
    DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    Queue ordenCreadaQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue ordenCreadaDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding ordenCreadaBinding() {
        return BindingBuilder.bind(ordenCreadaQueue()).to(pedidosExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding ordenCreadaDlqBinding() {
        return BindingBuilder.bind(ordenCreadaDlq()).to(deadLetterExchange()).with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

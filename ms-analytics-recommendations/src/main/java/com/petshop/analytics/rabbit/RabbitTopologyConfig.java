package com.petshop.analytics.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de este servicio: consume cupon.usado (lo publica ms-cupones).
 *
 *   pedidos.exchange (direct) --cupon.usado--> cupon.usado --(rechazo)--> pedidos.dlx --> cupon.usado.dlq
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitTopologyConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARG = "x-dead-letter-exchange";
    private static final String DEAD_LETTER_ROUTING_KEY_ARG = "x-dead-letter-routing-key";

    private final MessagingProperties props;

    public RabbitTopologyConfig(MessagingProperties props) {
        this.props = props;
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(props.exchanges().direct());
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dlx());
    }

    @Bean
    public Queue cuponUsadoQueue() {
        return QueueBuilder.durable(props.queues().cuponUsado())
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, props.exchanges().dlx())
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARG, props.dlqs().cuponUsado())
                .build();
    }

    @Bean
    public Queue cuponUsadoDlq() {
        return QueueBuilder.durable(props.dlqs().cuponUsado()).build();
    }

    @Bean
    public Binding cuponUsadoBinding() {
        return BindingBuilder.bind(cuponUsadoQueue()).to(directExchange()).with(props.routingKeys().cuponUsado());
    }

    @Bean
    public Binding cuponUsadoDlqBinding() {
        return BindingBuilder.bind(cuponUsadoDlq()).to(deadLetterExchange()).with(props.dlqs().cuponUsado());
    }
}

package com.petshop.orden.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ que este servicio posee. ms-orden PUBLICA orden.creada y stock.descontar (las colas las
 * declaran sus consumidores) y CONSUME pago.procesado, cuya cola y DLQ declara aqui:
 *
 *   pedidos.exchange (direct) --pago.procesado--> pago.procesado --(rechazo)--> pedidos.dlx --> pago.procesado.dlq
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
    public Queue pagoProcesadoQueue() {
        return QueueBuilder.durable(props.queues().pagoProcesado())
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, props.exchanges().dlx())
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARG, props.dlqs().pagoProcesado())
                .build();
    }

    @Bean
    public Queue pagoProcesadoDlq() {
        return QueueBuilder.durable(props.dlqs().pagoProcesado()).build();
    }

    @Bean
    public Binding pagoProcesadoBinding() {
        return BindingBuilder.bind(pagoProcesadoQueue()).to(directExchange()).with(props.routingKeys().pagoProcesado());
    }

    @Bean
    public Binding pagoProcesadoDlqBinding() {
        return BindingBuilder.bind(pagoProcesadoDlq()).to(deadLetterExchange()).with(props.dlqs().pagoProcesado());
    }
}

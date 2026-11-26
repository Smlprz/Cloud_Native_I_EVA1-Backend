package com.petshop.productos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ que este servicio posee. Solo declara beans (Queue, Exchange, Binding);
 * no contiene logica de negocio. Los nombres vienen de {@link MessagingProperties}.
 *
 *   pedidos.exchange (direct) --stock.descontar--> stock.descontar --(rechazo)--> pedidos.dlx --> stock.descontar.dlq
 *   pedidos.exchange (direct) --resena.creada-->   resena.creada   --(rechazo)--> pedidos.dlx --> resena.creada.dlq
 *
 * ms-productos tambien PUBLICA stock.bajo en el exchange topic pedidos.eventos (lo consume
 * ms-notificaciones, que declara esa cola), por eso declara tambien ese exchange.
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

    // ---------- Exchanges ----------

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(props.exchanges().direct());
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(props.exchanges().topic());
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dlx());
    }

    // ---------- Cola de descuento de stock (consumida por este servicio) y su DLQ ----------

    @Bean
    public Queue stockDescontarQueue() {
        return QueueBuilder.durable(props.queues().stockDescontar())
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, props.exchanges().dlx())
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARG, props.dlqs().stockDescontar())
                .build();
    }

    @Bean
    public Queue stockDescontarDlq() {
        return QueueBuilder.durable(props.dlqs().stockDescontar()).build();
    }

    @Bean
    public Binding stockDescontarBinding() {
        return BindingBuilder.bind(stockDescontarQueue())
                .to(directExchange())
                .with(props.routingKeys().stockDescontar());
    }

    @Bean
    public Binding stockDescontarDlqBinding() {
        return BindingBuilder.bind(stockDescontarDlq())
                .to(deadLetterExchange())
                .with(props.dlqs().stockDescontar());
    }

    // ---------- Cola de resenas nuevas (consumida por este servicio) y su DLQ ----------

    @Bean
    public Queue resenaCreadaQueue() {
        return QueueBuilder.durable(props.queues().resenaCreada())
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, props.exchanges().dlx())
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARG, props.dlqs().resenaCreada())
                .build();
    }

    @Bean
    public Queue resenaCreadaDlq() {
        return QueueBuilder.durable(props.dlqs().resenaCreada()).build();
    }

    @Bean
    public Binding resenaCreadaBinding() {
        return BindingBuilder.bind(resenaCreadaQueue())
                .to(directExchange())
                .with(props.routingKeys().resenaCreada());
    }

    @Bean
    public Binding resenaCreadaDlqBinding() {
        return BindingBuilder.bind(resenaCreadaDlq())
                .to(deadLetterExchange())
                .with(props.dlqs().resenaCreada());
    }
}

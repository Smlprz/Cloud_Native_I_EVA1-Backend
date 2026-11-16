package com.petshop.email.messaging.config;

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
 *   pedidos.exchange (direct) --orden.creada--> orden.creada --(rechazo)--> pedidos.dlx --> orden.creada.dlq
 *   pedidos.eventos  (topic)  --stock.bajo----> stock.bajo   --(rechazo)--> pedidos.dlx --> stock.bajo.dlq
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

    // ---------- Cola de ordenes creadas (correo de confirmacion) y su DLQ ----------

    @Bean
    public Queue ordenCreadaQueue() {
        return queueConDlq(props.queues().ordenCreada(), props.dlqs().ordenCreada());
    }

    @Bean
    public Queue ordenCreadaDlq() {
        return QueueBuilder.durable(props.dlqs().ordenCreada()).build();
    }

    @Bean
    public Binding ordenCreadaBinding() {
        return BindingBuilder.bind(ordenCreadaQueue()).to(directExchange()).with(props.routingKeys().ordenCreada());
    }

    @Bean
    public Binding ordenCreadaDlqBinding() {
        return BindingBuilder.bind(ordenCreadaDlq()).to(deadLetterExchange()).with(props.dlqs().ordenCreada());
    }

    // ---------- Cola de stock bajo (alerta al administrador) y su DLQ ----------

    @Bean
    public Queue stockBajoQueue() {
        return queueConDlq(props.queues().stockBajo(), props.dlqs().stockBajo());
    }

    @Bean
    public Queue stockBajoDlq() {
        return QueueBuilder.durable(props.dlqs().stockBajo()).build();
    }

    @Bean
    public Binding stockBajoBinding() {
        return BindingBuilder.bind(stockBajoQueue()).to(topicExchange()).with(props.routingKeys().stockBajo());
    }

    @Bean
    public Binding stockBajoDlqBinding() {
        return BindingBuilder.bind(stockBajoDlq()).to(deadLetterExchange()).with(props.dlqs().stockBajo());
    }

    /** Cola durable cuyos mensajes rechazados van al DLX con la routing key de su DLQ. */
    private Queue queueConDlq(String queue, String dlq) {
        return QueueBuilder.durable(queue)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, props.exchanges().dlx())
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARG, dlq)
                .build();
    }
}

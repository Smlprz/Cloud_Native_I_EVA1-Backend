package com.petshop.productos.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * Cliente RabbitMQ para publicar: serializa a JSON y deja registro en el log de los mensajes
 * que el broker no confirma o que no se pueden enrutar a ninguna cola.
 */
@Configuration
public class RabbitClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitClientConfig.class);

    /**
     * El convertidor JSON se usa SOLO para publicar. No se expone como bean MessageConverter: Spring
     * Boot lo aplicaria tambien a los consumidores, y al leer la cabecera __TypeId__ de ms-orden
     * (com.petshop.orden...) fallaria con ClassNotFoundException. Los consumidores reciben el cuerpo
     * en crudo y lo valida EventParser.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.error("Broker RECHAZO un mensaje id={} causa={}",
                        correlation != null ? correlation.getId() : "-", cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "Mensaje NO ENRUTADO (ninguna cola enlazada) exchange={} routingKey={} codigo={} motivo={} cuerpo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                returned.getReplyText(), new String(returned.getMessage().getBody(), StandardCharsets.UTF_8)));
        return template;
    }
}

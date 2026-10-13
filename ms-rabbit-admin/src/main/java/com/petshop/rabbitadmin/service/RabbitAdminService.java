package com.petshop.rabbitadmin.service;

import com.petshop.rabbitadmin.web.dto.BindingRequest;
import com.petshop.rabbitadmin.web.dto.ExchangeRequest;
import com.petshop.rabbitadmin.web.dto.QueueInfoResponse;
import com.petshop.rabbitadmin.web.dto.QueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Crea y elimina colas, exchanges y bindings en RabbitMQ. */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin admin;

    public RabbitAdminService(AmqpAdmin admin) {
        this.admin = admin;
    }

    public void crearExchange(ExchangeRequest r) {
        boolean durable = r.durable() == null || r.durable();
        String type = r.type() == null ? "direct" : r.type();
        AbstractExchange exchange = switch (type) {
            case "topic" -> new TopicExchange(r.name(), durable, false);
            case "fanout" -> new FanoutExchange(r.name(), durable, false);
            case "headers" -> new HeadersExchange(r.name(), durable, false);
            default -> new DirectExchange(r.name(), durable, false);
        };
        admin.declareExchange(exchange);
        log.info("Exchange '{}' ({}) creado", r.name(), type);
    }

    public void eliminarExchange(String name) {
        admin.deleteExchange(name);
        log.info("Exchange '{}' eliminado", name);
    }

    public void crearCola(QueueRequest r) {
        boolean durable = r.durable() == null || r.durable();
        QueueBuilder builder = durable ? QueueBuilder.durable(r.name()) : QueueBuilder.nonDurable(r.name());
        if (r.deadLetterExchange() != null && !r.deadLetterExchange().isBlank()) {
            builder.deadLetterExchange(r.deadLetterExchange());
            if (r.deadLetterRoutingKey() != null && !r.deadLetterRoutingKey().isBlank()) {
                builder.deadLetterRoutingKey(r.deadLetterRoutingKey());
            }
        }
        admin.declareQueue(builder.build());
        log.info("Cola '{}' creada", r.name());
    }

    public void eliminarCola(String name) {
        admin.deleteQueue(name);
        log.info("Cola '{}' eliminada", name);
    }

    public QueueInfoResponse infoCola(String name) {
        QueueInformation info = admin.getQueueInfo(name);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cola '" + name + "' no existe");
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void crearBinding(BindingRequest r) {
        admin.declareBinding(binding(r));
        log.info("Binding {} -> {} ({}) creado", r.exchange(), r.queue(), r.routingKey());
    }

    public void eliminarBinding(BindingRequest r) {
        admin.removeBinding(binding(r));
        log.info("Binding {} -> {} ({}) eliminado", r.exchange(), r.queue(), r.routingKey());
    }

    private Binding binding(BindingRequest r) {
        String routingKey = r.routingKey() == null ? "" : r.routingKey();
        return new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(), routingKey, null);
    }
}

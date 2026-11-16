package com.petshop.productos.messaging.stock;

import com.petshop.productos.messaging.config.MessagingProperties;
import com.petshop.productos.messaging.event.StockBajoEvent;
import com.petshop.productos.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica stock.bajo en el exchange topic pedidos.eventos. Es un aviso secundario: si RabbitMQ
 * falla, el descuento de stock ya hecho no se revierte, solo se registra el error.
 */
@Component
public class StockBajoPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockBajoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public StockBajoPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicar(StockService.StockBajo bajo) {
        StockBajoEvent evento = new StockBajoEvent(bajo.productoId(), bajo.nombre(), bajo.stock(), bajo.umbral());
        try {
            rabbitTemplate.convertAndSend(props.exchanges().topic(), props.routingKeys().stockBajo(), evento);
            log.info("Publicado '{}' del producto {} (stock {})",
                    props.routingKeys().stockBajo(), bajo.productoId(), bajo.stock());
        } catch (Exception e) {
            log.error("No se pudo publicar '{}' del producto {}: {}",
                    props.routingKeys().stockBajo(), bajo.productoId(), e.getMessage());
        }
    }
}

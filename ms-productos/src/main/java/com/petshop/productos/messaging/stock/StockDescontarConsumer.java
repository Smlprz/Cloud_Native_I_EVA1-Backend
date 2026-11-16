package com.petshop.productos.messaging.stock;

import com.petshop.productos.messaging.config.MessagingProperties;
import com.petshop.productos.messaging.event.StockDescontarCommand;
import com.petshop.productos.messaging.support.AckSupport;
import com.petshop.productos.messaging.support.EventParser;
import com.petshop.productos.messaging.support.NonRetryableMessageException;
import com.petshop.productos.service.StockInsuficienteException;
import com.petshop.productos.service.StockService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumidor de stock.descontar (cola stock.descontar). Descuenta el stock de la orden y, si
 * algun producto queda con stock bajo, publica stock.bajo. Sin stock suficiente es un error de
 * negocio: no se reintenta, el mensaje va a stock.descontar.dlq y queda en el log.
 * La confirmacion del mensaje (ACK / reintento / DLQ) la gestiona {@link AckSupport}.
 */
@Component
public class StockDescontarConsumer {

    static final String LISTENER_ID = "stock-descontar-listener";

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final StockService stockService;
    private final StockBajoPublisher stockBajoPublisher;
    private final MessagingProperties props;

    public StockDescontarConsumer(EventParser parser, AckSupport ackSupport, StockService stockService,
                                  StockBajoPublisher stockBajoPublisher, MessagingProperties props) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.stockService = stockService;
        this.stockBajoPublisher = stockBajoPublisher;
        this.props = props;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.stock-descontar}")
    public void onStockDescontar(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().stockDescontar(), message, channel, () -> {
            StockDescontarCommand comando = parser.parse(message, StockDescontarCommand.class);
            List<StockService.StockBajo> bajos;
            try {
                bajos = stockService.descontar(comando.ordenId(), comando.items().stream()
                        .map(l -> new StockService.Linea(l.productoId(), l.cantidad()))
                        .toList());
            } catch (StockInsuficienteException e) {
                throw new NonRetryableMessageException(e.getMessage(), e);
            }
            bajos.forEach(stockBajoPublisher::publicar);
        });
    }
}

package com.petshop.productos.messaging.stock;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.productos.messaging.config.MessagingProperties;
import com.petshop.productos.messaging.support.AckSupport;
import com.petshop.productos.messaging.support.EventParser;
import com.petshop.productos.service.StockInsuficienteException;
import com.petshop.productos.service.StockService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Politica de ACK / reintento / DLQ del consumidor de stock.descontar. */
class StockDescontarConsumerTest {

    private static final long TAG = 3L;

    private static final String COMANDO_VALIDO = """
            {"ordenId":42,"items":[{"productoId":10,"cantidad":2},{"productoId":11,"cantidad":1}]}
            """;

    private StockService stockService;
    private StockBajoPublisher stockBajoPublisher;
    private Channel channel;
    private StockDescontarConsumer consumer;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        stockBajoPublisher = mock(StockBajoPublisher.class);
        channel = mock(Channel.class);

        MessagingProperties props = new MessagingProperties(
                new MessagingProperties.Exchanges("pedidos.exchange", "pedidos.eventos", "pedidos.dlx"),
                new MessagingProperties.Queues("orden.creada", "stock.descontar", "stock.bajo", "resena.creada"),
                new MessagingProperties.Dlqs("orden.creada.dlq", "stock.descontar.dlq", "stock.bajo.dlq", "resena.creada.dlq"),
                new MessagingProperties.RoutingKeys("orden.creada", "stock.descontar", "stock.bajo", "resena.creada"),
                new MessagingProperties.Retry(3, 1));

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        EventParser parser = new EventParser(mapper, Validation.buildDefaultValidatorFactory().getValidator());
        consumer = new StockDescontarConsumer(parser, new AckSupport(props), stockService, stockBajoPublisher, props);
    }

    @Test
    void comandoValidoDescuentaElStockYHaceAck() throws Exception {
        when(stockService.descontar(any(), any())).thenReturn(List.of());

        consumer.onStockDescontar(mensaje(COMANDO_VALIDO), channel);

        verify(stockService).descontar(42L, List.of(
                new StockService.Linea(10L, 2), new StockService.Linea(11L, 1)));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
        verify(stockBajoPublisher, never()).publicar(any());
    }

    @Test
    void publicaStockBajoPorCadaProductoQueQuedoBajoElUmbral() throws Exception {
        StockService.StockBajo bajo = new StockService.StockBajo(10L, "Collar", 3, 5);
        when(stockService.descontar(any(), any())).thenReturn(List.of(bajo));

        consumer.onStockDescontar(mensaje(COMANDO_VALIDO), channel);

        verify(stockBajoPublisher).publicar(bajo);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void stockInsuficienteVaADlqSinReintentos() throws Exception {
        when(stockService.descontar(any(), any())).thenThrow(new StockInsuficienteException("sin stock"));

        consumer.onStockDescontar(mensaje(COMANDO_VALIDO), channel);

        verify(stockService, times(1)).descontar(any(), any());
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(any(Long.class), any(Boolean.class));
        verify(stockBajoPublisher, never()).publicar(any());
    }

    @Test
    void jsonInvalidoVaADlqSinTocarElStock() throws Exception {
        consumer.onStockDescontar(mensaje("[1,2,3"), channel);

        verify(stockService, never()).descontar(any(), any());
        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void cantidadNegativaEsInvalidaYVaADlq() throws Exception {
        String invalido = COMANDO_VALIDO.replace("\"cantidad\":2", "\"cantidad\":-2");

        consumer.onStockDescontar(mensaje(invalido), channel);

        verify(stockService, never()).descontar(any(), any());
        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void errorTecnicoSeReintentaYAgotadosVaALaDlq() throws Exception {
        when(stockService.descontar(any(), any())).thenThrow(new IllegalStateException("base de datos caida"));

        consumer.onStockDescontar(mensaje(COMANDO_VALIDO), channel);

        verify(stockService, times(3)).descontar(any(), any());
        verify(channel).basicNack(TAG, false, false);
    }

    private static Message mensaje(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(TAG);
        properties.setMessageId("m-3");
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }
}

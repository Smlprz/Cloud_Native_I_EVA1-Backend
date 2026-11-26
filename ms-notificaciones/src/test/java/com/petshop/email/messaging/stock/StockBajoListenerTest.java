package com.petshop.email.messaging.stock;

import com.petshop.email.messaging.config.MessagingProperties;
import com.petshop.email.messaging.support.AckSupport;
import com.petshop.email.messaging.support.EventParser;
import com.petshop.email.service.EmailService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Politica de ACK / reintento / DLQ del consumidor de stock.bajo. */
class StockBajoListenerTest {

    private static final long TAG = 11L;

    private static final String EVENTO_VALIDO =
            "{\"productoId\":3,\"nombre\":\"Collar Perro\",\"stock\":2,\"umbral\":5}";

    private final MessagingProperties props = new MessagingProperties(
            new MessagingProperties.Exchanges("pedidos.exchange", "pedidos.eventos", "pedidos.dlx"),
            new MessagingProperties.Queues("orden.creada", "stock.descontar", "stock.bajo", "resena.creada"),
            new MessagingProperties.Dlqs("orden.creada.dlq", "stock.descontar.dlq", "stock.bajo.dlq", "resena.creada.dlq"),
            new MessagingProperties.RoutingKeys("orden.creada", "stock.descontar", "stock.bajo", "resena.creada"),
            new MessagingProperties.Retry(3, 1));

    private EmailService emailService;
    private Channel channel;
    private EventParser parser;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        channel = mock(Channel.class);
        parser = new EventParser(JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    private StockBajoListener listener(String correoAlerta) {
        return new StockBajoListener(parser, new AckSupport(props), emailService, props, correoAlerta);
    }

    @Test
    void enviaLaAlertaAlAdministradorYHaceAck() throws Exception {
        listener("admin@tienda.cl").onStockBajo(mensaje(EVENTO_VALIDO), channel);

        verify(emailService).enviarCorreo(eq("admin@tienda.cl"), contains("Collar Perro"), contains("2 unidad"));
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void sinCorreoDeAlertaSoloRegistraEnElLogYHaceAck() throws Exception {
        listener("").onStockBajo(mensaje(EVENTO_VALIDO), channel);

        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void mensajeInvalidoVaADlqSinReintentos() throws Exception {
        listener("admin@tienda.cl").onStockBajo(mensaje("{\"productoId\":3,\"stock\":-4}"), channel);

        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void siElSmtpFallaSeReintentaYAgotadosVaALaDlq() throws Exception {
        doThrow(new RuntimeException("smtp caido")).when(emailService).enviarCorreo(anyString(), anyString(), anyString());

        listener("admin@tienda.cl").onStockBajo(mensaje(EVENTO_VALIDO), channel);

        verify(emailService, times(3)).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(any(Long.class), any(Boolean.class));
    }

    private static Message mensaje(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(TAG);
        properties.setMessageId("m-11");
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }
}

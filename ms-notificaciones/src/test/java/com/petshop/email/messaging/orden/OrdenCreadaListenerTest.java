package com.petshop.email.messaging.orden;

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

/** Politica de ACK / reintento / DLQ del consumidor de orden.creada. */
class OrdenCreadaListenerTest {

    private static final long TAG = 5L;

    private static final String EVENTO_VALIDO =
            "{\"ordenId\":7,\"email\":\"cliente@correo.cl\",\"total\":15990}";

    private EmailService emailService;
    private Channel channel;
    private OrdenCreadaListener listener;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        channel = mock(Channel.class);
        MessagingProperties props = new MessagingProperties(
                new MessagingProperties.Exchanges("pedidos.exchange", "pedidos.eventos", "pedidos.dlx"),
                new MessagingProperties.Queues("orden.creada", "stock.descontar", "stock.bajo", "resena.creada", "pago.procesado", "cupon.usado"),
                new MessagingProperties.Dlqs("orden.creada.dlq", "stock.descontar.dlq", "stock.bajo.dlq", "resena.creada.dlq", "pago.procesado.dlq", "cupon.usado.dlq"),
                new MessagingProperties.RoutingKeys("orden.creada", "stock.descontar", "stock.bajo", "resena.creada", "pago.procesado", "cupon.usado"),
                new MessagingProperties.Retry(3, 1));
        EventParser parser = new EventParser(JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator());
        listener = new OrdenCreadaListener(parser, new AckSupport(props), emailService, props);
    }

    @Test
    void alRecibirUnaOrdenEnviaElCorreoYHaceAck() throws Exception {
        listener.onOrdenCreada(mensaje(EVENTO_VALIDO), channel);

        verify(emailService).enviarCorreo(eq("cliente@correo.cl"), contains("7"), contains("15990"));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void siElCorreoFallaSeReintentaYAgotadosVaALaDlq() throws Exception {
        doThrow(new RuntimeException("smtp caido")).when(emailService).enviarCorreo(anyString(), anyString(), anyString());

        listener.onOrdenCreada(mensaje(EVENTO_VALIDO), channel);

        verify(emailService, times(3)).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(any(Long.class), any(Boolean.class));
    }

    @Test
    void jsonInvalidoVaADlqSinReintentos() throws Exception {
        listener.onOrdenCreada(mensaje("{{{ no es json"), channel);

        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void ordenSinEmailDeDestinoVaADlqSinReintentos() throws Exception {
        listener.onOrdenCreada(mensaje("{\"ordenId\":8,\"email\":\"\",\"total\":100}"), channel);

        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        verify(channel).basicNack(TAG, false, false);
    }

    private static Message mensaje(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(TAG);
        properties.setMessageId("m-5");
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }
}

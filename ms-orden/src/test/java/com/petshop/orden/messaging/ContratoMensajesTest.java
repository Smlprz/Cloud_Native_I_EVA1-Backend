package com.petshop.orden.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato con los consumidores: ms-notificaciones espera {ordenId, email, total} y ms-productos
 * espera {ordenId, items:[{productoId, cantidad}]}. Si alguien renombra un campo de estos records,
 * este test falla antes de que los mensajes lleguen a la DLQ en produccion.
 */
class ContratoMensajesTest {

    private final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode jsonDe(Object payload) throws IOException {
        Message mensaje = converter.toMessage(payload, new MessageProperties());
        return mapper.readTree(mensaje.getBody());
    }

    @Test
    void ordenCreadaTieneLosCamposQueConsumeNotificaciones() throws IOException {
        JsonNode json = jsonDe(new OrdenCreadaEvent(7L, "cliente@correo.cl", new BigDecimal("15990")));

        assertEquals(7L, json.get("ordenId").asLong());
        assertEquals("cliente@correo.cl", json.get("email").asText());
        assertEquals(15990, json.get("total").asInt());
    }

    @Test
    void stockDescontarTieneLosCamposQueConsumeProductos() throws IOException {
        JsonNode json = jsonDe(new StockDescontarCommand(9L,
                List.of(new StockDescontarCommand.Linea(5L, 2), new StockDescontarCommand.Linea(6L, 1))));

        assertEquals(9L, json.get("ordenId").asLong());
        assertTrue(json.get("items").isArray());
        assertEquals(2, json.get("items").size());
        assertEquals(5L, json.get("items").get(0).get("productoId").asLong());
        assertEquals(2, json.get("items").get(0).get("cantidad").asInt());
    }
}

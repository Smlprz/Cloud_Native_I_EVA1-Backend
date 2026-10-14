package com.petshop.rabbitadmin.service;

import com.petshop.rabbitadmin.web.dto.BindingRequest;
import com.petshop.rabbitadmin.web.dto.ExchangeRequest;
import com.petshop.rabbitadmin.web.dto.QueueInfoResponse;
import com.petshop.rabbitadmin.web.dto.QueueRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prueba de integracion contra un RabbitMQ REAL (localhost:5672, guest/guest; por ejemplo el
 * contenedor de infra/docker-compose.yml). Si no hay broker, la prueba se omite y no falla.
 * Recorre lo mismo que la pantalla de administracion: crear, consultar y eliminar.
 */
class RabbitAdminServiceBrokerTest {

    private CachingConnectionFactory conexion;
    private RabbitAdminService servicio;
    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private final String cola = "test.cola." + sufijo;
    private final String exchange = "test.exchange." + sufijo;

    @BeforeEach
    void conectar() {
        conexion = new CachingConnectionFactory("localhost", 5672);
        conexion.setUsername("guest");
        conexion.setPassword("guest");
        boolean disponible;
        try {
            conexion.createConnection().close();
            disponible = true;
        } catch (Exception e) {
            disponible = false;
        }
        assumeTrue(disponible, "No hay RabbitMQ en localhost:5672: prueba omitida");
        servicio = new RabbitAdminService(new RabbitAdmin(conexion));
    }

    @AfterEach
    void limpiar() {
        if (servicio != null) {
            servicio.eliminarCola(cola);
            servicio.eliminarExchange(exchange);
        }
        if (conexion != null) conexion.destroy();
    }

    @Test
    void consultarUnaColaQueNoExisteDevuelve404() {
        ResponseStatusException error =
                assertThrows(ResponseStatusException.class, () -> servicio.infoCola(cola));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void crearConsultarYEliminarColaExchangeYBinding() {
        servicio.crearExchange(new ExchangeRequest(exchange, "direct", true));
        servicio.crearCola(new QueueRequest(cola, true, null, null));
        servicio.crearBinding(new BindingRequest(cola, exchange, "clave"));

        QueueInfoResponse info = servicio.infoCola(cola);
        assertEquals(cola, info.name());
        assertEquals(0, info.mensajes());

        servicio.eliminarBinding(new BindingRequest(cola, exchange, "clave"));
        servicio.eliminarCola(cola);

        ResponseStatusException error =
                assertThrows(ResponseStatusException.class, () -> servicio.infoCola(cola));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void unaColaConDlqSeCreaYSeConsulta() {
        servicio.crearExchange(new ExchangeRequest(exchange, "direct", true));
        servicio.crearCola(new QueueRequest(cola, true, exchange, "rk.muerto"));

        assertEquals(cola, servicio.infoCola(cola).name());
    }
}

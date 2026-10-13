package com.petshop.rabbitadmin.web;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AmqpAdmin amqpAdmin;

    private static RequestPostProcessor admin() {
        return jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_rabbit.admin"),
                new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/rabbit/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteSinRolAdminDevuelve403() throws Exception {
        mvc.perform(post("/api/rabbit/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_rabbit.admin"),
                                new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaExchangeColaYBinding() throws Exception {
        mvc.perform(post("/api/rabbit/exchanges").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ex.test\",\"type\":\"direct\"}"))
                .andExpect(status().isCreated());
        verify(amqpAdmin).declareExchange(any(DirectExchange.class));

        mvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q.test\",\"deadLetterExchange\":\"dlx\",\"deadLetterRoutingKey\":\"q.test.dlq\"}"))
                .andExpect(status().isCreated());
        verify(amqpAdmin).declareQueue(any(Queue.class));

        mvc.perform(post("/api/rabbit/bindings").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queue\":\"q.test\",\"exchange\":\"ex.test\",\"routingKey\":\"rk\"}"))
                .andExpect(status().isCreated());
        verify(amqpAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void adminEliminaColaExchangeYBinding() throws Exception {
        mvc.perform(delete("/api/rabbit/queues/q.test").with(admin())).andExpect(status().isNoContent());
        verify(amqpAdmin).deleteQueue(eq("q.test"));

        mvc.perform(delete("/api/rabbit/exchanges/ex.test").with(admin())).andExpect(status().isNoContent());
        verify(amqpAdmin).deleteExchange(eq("ex.test"));

        mvc.perform(delete("/api/rabbit/bindings").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queue\":\"q.test\",\"exchange\":\"ex.test\",\"routingKey\":\"rk\"}"))
                .andExpect(status().isNoContent());
        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void tipoDeExchangeInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/rabbit/exchanges").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"type\":\"raro\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void colaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/rabbit/queues/no-existe").with(admin())).andExpect(status().isNotFound());
    }
}

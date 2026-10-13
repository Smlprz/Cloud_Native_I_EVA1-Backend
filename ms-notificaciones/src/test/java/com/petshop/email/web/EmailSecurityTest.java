package com.petshop.email.web;

import com.petshop.email.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class EmailSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailService emailService;

    @Test
    void sinTokenDevuelve401YNoEnviaCorreo() throws Exception {
        mvc.perform(post("/api/email/enviar")
                        .param("to", "a@b.cl").param("subject", "s").param("body", "b"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailService);
    }

    @Test
    void conTokenSinRolAdminDevuelve403() throws Exception {
        mvc.perform(post("/api/email/enviar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .param("to", "a@b.cl").param("subject", "s").param("body", "b"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(emailService);
    }

    @Test
    void adminPuedeEnviarCorreo() throws Exception {
        mvc.perform(post("/api/email/enviar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("to", "a@b.cl").param("subject", "s").param("body", "b"))
                .andExpect(status().isOk());

        verify(emailService).enviarCorreo("a@b.cl", "s", "b");
    }
}

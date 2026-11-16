package com.petshop.email.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void enModoSimuladoNoAbreConexionSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "tienda@test.cl", true, "");

        service.enviarCorreo("cliente@correo.cl", "Asunto", "Cuerpo");

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enModoRealEnviaPorSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "tienda@test.cl", false, "");

        service.enviarCorreo("cliente@correo.cl", "Asunto", "Cuerpo");

        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void conDestinoForzadoEntregaAEsaDireccion() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "tienda@test.cl", false, "demo@gmail.com");

        service.enviarCorreo("sin-buzon@tenant.onmicrosoft.com", "Asunto", "Cuerpo");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertArrayEquals(new String[]{"demo@gmail.com"}, captor.getValue().getTo());
        assertTrue(captor.getValue().getText().contains("sin-buzon@tenant.onmicrosoft.com"));
    }
}

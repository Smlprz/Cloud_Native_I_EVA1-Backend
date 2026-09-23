package com.petshop.email.web;

import com.petshop.email.service.EmailService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Test unitario simple (sin levantar contexto de Spring): instancia el
 * controlador a mano con un EmailService mockeado.
 */
class EmailControllerTest {

    @Test
    void enviarCorreoDevuelveMensajeDeExito() {
        EmailService emailService = mock(EmailService.class);
        doNothing().when(emailService).enviarCorreo(anyString(), anyString(), anyString());
        EmailController controller = new EmailController(emailService);

        String resultado = controller.enviarCorreoPrueba("destino@test.cl", "Asunto", "Cuerpo");

        assertThat(resultado).isEqualTo("Correo enviado exitosamente a destino@test.cl");
    }

    @Test
    void siFallaElEnvioDevuelveMensajeDeError() {
        EmailService emailService = mock(EmailService.class);
        doThrow(new RuntimeException("smtp caido")).when(emailService)
                .enviarCorreo(anyString(), anyString(), anyString());
        EmailController controller = new EmailController(emailService);

        String resultado = controller.enviarCorreoPrueba("destino@test.cl", "Asunto", "Cuerpo");

        assertThat(resultado).isEqualTo("Error al enviar el correo: smtp caido");
    }
}

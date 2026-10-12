package com.petshop.email.messaging;

import com.petshop.email.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class OrdenCreadaListenerTest {

    private final EmailService emailService = Mockito.mock(EmailService.class);
    private final OrdenCreadaListener listener = new OrdenCreadaListener(emailService);

    @Test
    void alRecibirUnaOrdenEnviaElCorreo() {
        listener.onOrdenCreada(new OrdenCreadaEvent(7L, "cliente@correo.cl", new BigDecimal("15990")));

        verify(emailService).enviarCorreo(eq("cliente@correo.cl"), contains("7"), contains("15990"));
    }

    @Test
    void siElCorreoFallaPropagaLaExcepcionParaQueVayaALaDlq() {
        doThrow(new RuntimeException("smtp caido")).when(emailService).enviarCorreo(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class,
                () -> listener.onOrdenCreada(new OrdenCreadaEvent(8L, "x@y.cl", BigDecimal.ONE)));
    }
}

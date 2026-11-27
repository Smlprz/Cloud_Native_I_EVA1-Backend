package com.petshop.analytics.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los mensajes llegan de OTRO microservicio con la cabecera __TypeId__ de su clase Java. Si existiera un bean
 * MessageConverter, Spring Boot lo usaria en los consumidores y fallaria con ClassNotFoundException (el mensaje
 * terminaria en la DLQ). Los consumidores reciben el cuerpo en crudo y lo valida EventParser.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConvertidorRabbitTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void noHayUnConvertidorGlobalQueLeaLaCabeceraTypeId() {
        assertThat(contexto.getBeanNamesForType(MessageConverter.class)).isEmpty();
    }
}

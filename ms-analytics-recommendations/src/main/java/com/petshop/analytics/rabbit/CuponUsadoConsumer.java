package com.petshop.analytics.rabbit;

import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Metrics;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Consumidor de cupon.usado: cuenta los cupones usados (metrica pedidos360_cupones_usados_total). */
@Component
public class CuponUsadoConsumer {

    static final String LISTENER_ID = "cupon-usado-listener";
    private static final Logger log = LoggerFactory.getLogger(CuponUsadoConsumer.class);

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final MessagingProperties props;

    public CuponUsadoConsumer(EventParser parser, AckSupport ackSupport, MessagingProperties props) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.props = props;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.cupon-usado}")
    public void onCuponUsado(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().cuponUsado(), message, channel, () -> {
            CuponUsadoEvent evento = parser.parse(message, CuponUsadoEvent.class);
            Metrics.counter("pedidos360.cupones.usados", "codigo", evento.codigo()).increment();
            log.info("Cupon {} usado (descuento {})", evento.codigo(), evento.descuento());
        });
    }

    public record CuponUsadoEvent(@NotBlank String codigo, String usuarioId, BigDecimal descuento) {
    }
}

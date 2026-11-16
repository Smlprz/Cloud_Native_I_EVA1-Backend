package com.petshop.email.messaging.stock;

import com.petshop.email.messaging.config.MessagingProperties;
import com.petshop.email.messaging.support.AckSupport;
import com.petshop.email.messaging.support.EventParser;
import com.petshop.email.service.EmailService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumidor de stock.bajo (exchange topic pedidos.eventos): avisa por correo al administrador
 * que un producto se esta agotando. Si no hay correo de alerta configurado (app.stock.alert-email)
 * solo se registra el aviso en el log y el mensaje se confirma.
 */
@Component
public class StockBajoListener {

    static final String LISTENER_ID = "stock-bajo-listener";

    private static final Logger log = LoggerFactory.getLogger(StockBajoListener.class);

    private final EventParser parser;
    private final AckSupport ackSupport;
    private final EmailService emailService;
    private final MessagingProperties props;
    private final String correoAlerta;

    public StockBajoListener(EventParser parser, AckSupport ackSupport, EmailService emailService,
                             MessagingProperties props, @Value("${app.stock.alert-email:}") String correoAlerta) {
        this.parser = parser;
        this.ackSupport = ackSupport;
        this.emailService = emailService;
        this.props = props;
        this.correoAlerta = correoAlerta;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${messaging.queues.stock-bajo}")
    public void onStockBajo(Message message, Channel channel) {
        ackSupport.handle(LISTENER_ID, props.dlqs().stockBajo(), message, channel, () -> {
            StockBajoEvent evento = parser.parse(message, StockBajoEvent.class);
            if (!StringUtils.hasText(correoAlerta)) {
                log.warn("Stock bajo de '{}' ({} unidades, umbral {}) pero no hay correo de alerta configurado "
                                + "(app.stock.alert-email): no se envia correo",
                        evento.nombre(), evento.stock(), evento.umbral());
                return;
            }
            emailService.enviarCorreo(
                    correoAlerta,
                    "Alerta de stock bajo: " + evento.nombre(),
                    "El producto '" + evento.nombre() + "' (id " + evento.productoId() + ") quedo con "
                            + evento.stock() + " unidad(es) en stock (umbral de alerta: " + evento.umbral() + ").\n"
                            + "Conviene reponerlo.");
        });
    }
}

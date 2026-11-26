package com.petshop.pago.service;

import com.petshop.pago.antifraude.Antifraude;
import com.petshop.pago.client.OrdenDto;
import com.petshop.pago.client.OrdenesClient;
import com.petshop.pago.domain.AlertaFraude;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import com.petshop.pago.domain.WebpayTransaccion;
import com.petshop.pago.messaging.PagoEventPublisher;
import com.petshop.pago.repository.AlertaFraudeRepository;
import com.petshop.pago.repository.PagoRepository;
import com.petshop.pago.repository.WebpayTransaccionRepository;
import com.petshop.pago.web.dto.PagoResponse;
import com.petshop.pago.web.dto.WebpayInicioResponse;
import com.petshop.pago.webpay.WebpayClient;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;

/**
 * Pago con Webpay Plus.
 *
 * Como la tarjeta se escribe en el sitio de Transbank y el comercio nunca la ve, la regla antifraude se aplica
 * sobre el ID DEL USUARIO: mas de 3 inicios de pago del mismo usuario en 10 segundos se bloquean.
 *
 *  iniciar():   verifica la orden, aplica el antifraude, crea la transaccion y devuelve token + URL.
 *  confirmar(): al volver de Webpay hace el commit, registra el pago y publica el evento en eventos.pagos.
 *  anular():    el usuario cancelo en el formulario de Webpay.
 */
@Service
public class WebpayService {

    private static final Logger log = LoggerFactory.getLogger(WebpayService.class);
    /** Valor de "ultimos 4" cuando Webpay aun no informa la tarjeta. */
    private static final String SIN_TARJETA = "WPAY";

    private final WebpayClient webpay;
    private final WebpayTransaccionRepository transacciones;
    private final PagoRepository pagos;
    private final AlertaFraudeRepository alertas;
    private final OrdenesClient ordenes;
    private final Antifraude antifraude;
    private final PagoEventPublisher publicador;
    private final String pepper;
    private final String returnUrl;

    public WebpayService(WebpayClient webpay, WebpayTransaccionRepository transacciones, PagoRepository pagos,
                         AlertaFraudeRepository alertas, OrdenesClient ordenes, Antifraude antifraude,
                         PagoEventPublisher publicador,
                         @Value("${app.antifraude.pepper}") String pepper,
                         @Value("${app.webpay.return-url}") String returnUrl) {
        this.webpay = webpay;
        this.transacciones = transacciones;
        this.pagos = pagos;
        this.alertas = alertas;
        this.ordenes = ordenes;
        this.antifraude = antifraude;
        this.publicador = publicador;
        this.pepper = pepper;
        this.returnUrl = returnUrl;
    }

    public WebpayInicioResponse iniciar(String usuarioId, String bearerToken, Long ordenId) {
        OrdenDto orden = ordenes.buscar(ordenId, bearerToken)
                .orElseThrow(() -> PagoException.ordenNoEncontrada(ordenId));
        String huellaUsuario = TarjetaUtil.hash("usuario:" + usuarioId, pepper);

        Antifraude.Resultado intento = antifraude.evaluar(usuarioId, orden.id());
        if (intento.bloqueado()) {
            String motivo = antifraude.motivo(intento.hasta());
            alertas.save(new AlertaFraude(usuarioId, orden.id(), SIN_TARJETA, huellaUsuario,
                    antifraude.getMaxIntentos() + 1, antifraude.getVentanaSegundos()));
            Pago bloqueado = pagos.save(new Pago(orden.id(), usuarioId, orden.total(), SIN_TARJETA, huellaUsuario,
                    EstadoPago.BLOQUEADO_FRAUDE, motivo));
            Metrics.counter("pedidos360.pagos", "estado", EstadoPago.BLOQUEADO_FRAUDE.name()).increment();
            publicador.publicar(bloqueado);
            log.warn("Pago Webpay bloqueado por antifraude: usuario {} (bloqueado hasta {}), orden {}",
                    usuarioId, intento.hasta(), orden.id());
            throw PagoException.bloqueadoPorFraude(motivo);
        }
        if (pagos.existsByOrdenIdAndEstado(orden.id(), EstadoPago.APROBADO)) {
            throw PagoException.ordenYaPagada(orden.id());
        }

        String buyOrder = "O" + orden.id() + "-" + System.currentTimeMillis();
        long monto = orden.total().setScale(0, RoundingMode.HALF_UP).longValueExact(); // pesos, sin decimales
        WebpayClient.Creada creada = webpay.crear(buyOrder, huellaUsuario.substring(0, 40), monto, returnUrl);
        transacciones.save(new WebpayTransaccion(creada.token(), orden.id(), usuarioId, orden.total(), buyOrder));
        log.info("Webpay iniciado para la orden {} (monto {})", orden.id(), monto);
        return new WebpayInicioResponse(creada.url(), creada.token());
    }

    public synchronized PagoResponse confirmar(String usuarioId, String token) {
        WebpayTransaccion tx = propia(usuarioId, token);
        if (tx.getPagoId() != null) {
            return PagoResponse.from(pagos.findById(tx.getPagoId()).orElseThrow()); // recarga de la pagina
        }
        WebpayClient.Confirmacion c = webpay.confirmar(token);
        long esperado = tx.getMonto().setScale(0, RoundingMode.HALF_UP).longValueExact();
        boolean aprobado = c.autorizada() && c.amount() == esperado && tx.getBuyOrder().equals(c.buyOrder());
        return registrar(tx, c.ultimos4(), aprobado ? EstadoPago.APROBADO : EstadoPago.RECHAZADO,
                aprobado ? null : "Webpay rechazo la transaccion");
    }

    public synchronized PagoResponse anular(String usuarioId, String token) {
        WebpayTransaccion tx = propia(usuarioId, token);
        if (tx.getPagoId() != null) {
            return PagoResponse.from(pagos.findById(tx.getPagoId()).orElseThrow());
        }
        return registrar(tx, SIN_TARJETA, EstadoPago.RECHAZADO, "Pago anulado en Webpay");
    }

    private WebpayTransaccion propia(String usuarioId, String token) {
        return transacciones.findById(token)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(PagoException::transaccionNoEncontrada);
    }

    private PagoResponse registrar(WebpayTransaccion tx, String ultimos4, EstadoPago estado, String motivo) {
        Pago pago = pagos.save(new Pago(tx.getOrdenId(), tx.getUsuarioId(), tx.getMonto(), ultimos4,
                TarjetaUtil.hash("usuario:" + tx.getUsuarioId(), pepper), estado, motivo));
        tx.finalizar(pago.getId());
        transacciones.save(tx);
        log.info("Pago Webpay {} de la orden {}: {}", pago.getId(), tx.getOrdenId(), estado);
        publicador.publicar(pago);
        return PagoResponse.from(pago);
    }
}

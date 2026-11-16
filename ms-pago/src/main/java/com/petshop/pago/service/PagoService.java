package com.petshop.pago.service;

import com.petshop.pago.antifraude.DetectorFraude;
import com.petshop.pago.client.OrdenDto;
import com.petshop.pago.client.OrdenesClient;
import com.petshop.pago.domain.AlertaFraude;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import com.petshop.pago.messaging.PagoEventPublisher;
import com.petshop.pago.repository.AlertaFraudeRepository;
import com.petshop.pago.repository.PagoRepository;
import com.petshop.pago.web.dto.AlertaFraudeResponse;
import com.petshop.pago.web.dto.PagoRequest;
import com.petshop.pago.web.dto.PagoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

/**
 * Pago simulado de una orden, con control antifraude.
 *
 * Pasos de procesar():
 *  1. La orden se consulta en ms-orden con el JWT del usuario (asi el monto sale de la orden y solo se
 *     paga una orden propia).
 *  2. Se registra el intento en el detector antifraude: la misma tarjeta mas de 3 veces en 10 s se BLOQUEA,
 *     se guarda una alerta y se deja un WARN en el log.
 *  3. Si no esta bloqueada y la orden no estaba pagada, se valida la tarjeta (Luhn, vencimiento) y se
 *     aprueba o rechaza. Tarjeta de prueba terminada en 0002: "fondos insuficientes".
 *  4. El resultado se guarda (solo ultimos 4 digitos + hash) y se publica en Kafka (eventos.pagos).
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagos;
    private final AlertaFraudeRepository alertas;
    private final OrdenesClient ordenes;
    private final DetectorFraude detector;
    private final PagoEventPublisher publicador;
    private final String pepper;

    public PagoService(PagoRepository pagos, AlertaFraudeRepository alertas, OrdenesClient ordenes,
                       DetectorFraude detector, PagoEventPublisher publicador,
                       @Value("${app.antifraude.pepper}") String pepper) {
        this.pagos = pagos;
        this.alertas = alertas;
        this.ordenes = ordenes;
        this.detector = detector;
        this.publicador = publicador;
        this.pepper = pepper;
    }

    public PagoResponse procesar(String usuarioId, String bearerToken, PagoRequest request) {
        OrdenDto orden = ordenes.buscar(request.ordenId(), bearerToken)
                .orElseThrow(() -> PagoException.ordenNoEncontrada(request.ordenId()));

        String tarjeta = TarjetaUtil.normalizar(request.numeroTarjeta());
        String ultimos4 = TarjetaUtil.ultimos4(tarjeta);
        String hash = TarjetaUtil.hash(tarjeta, pepper);

        // Todo intento cuenta para la regla antifraude, incluso si la orden ya estaba pagada.
        DetectorFraude.Resultado intento = detector.registrarIntento(hash);

        EstadoPago estado;
        String motivo = null;
        if (intento.bloqueado()) {
            estado = EstadoPago.BLOQUEADO_FRAUDE;
            motivo = "La misma tarjeta se uso " + intento.intentosEnVentana() + " veces en "
                    + detector.getVentanaSegundos() + " segundos: transaccion bloqueada por seguridad";
            alertas.save(new AlertaFraude(usuarioId, orden.id(), ultimos4, hash,
                    intento.intentosEnVentana(), detector.getVentanaSegundos()));
            log.warn("ALERTA DE FRAUDE: tarjeta ****{} (hash {}...) con {} intentos en {} s, orden {}, usuario {}. Transaccion bloqueada.",
                    ultimos4, hash.substring(0, 8), intento.intentosEnVentana(), detector.getVentanaSegundos(),
                    orden.id(), usuarioId);
        } else if (pagos.existsByOrdenIdAndEstado(orden.id(), EstadoPago.APROBADO)) {
            throw PagoException.ordenYaPagada(orden.id());
        } else if (!TarjetaUtil.luhnValido(tarjeta)) {
            estado = EstadoPago.RECHAZADO;
            motivo = "Numero de tarjeta invalido";
        } else if (TarjetaUtil.vencida(request.vencimiento(), YearMonth.now())) {
            estado = EstadoPago.RECHAZADO;
            motivo = "Tarjeta vencida";
        } else if (tarjeta.endsWith("0002")) {
            estado = EstadoPago.RECHAZADO;
            motivo = "Fondos insuficientes";
        } else {
            estado = EstadoPago.APROBADO;
        }

        Pago pago = pagos.save(new Pago(orden.id(), usuarioId, orden.total(), ultimos4, hash, estado, motivo));
        log.info("Pago {} de la orden {}: {} (tarjeta ****{})", pago.getId(), orden.id(), estado, ultimos4);
        publicador.publicar(pago);
        return PagoResponse.from(pago);
    }

    public List<PagoResponse> listar(String usuarioId, Long ordenId) {
        List<Pago> lista = ordenId != null
                ? pagos.findByUsuarioIdAndOrdenIdOrderByCreadoEnDesc(usuarioId, ordenId)
                : pagos.findByUsuarioIdOrderByCreadoEnDesc(usuarioId);
        return lista.stream().map(PagoResponse::from).toList();
    }

    public List<AlertaFraudeResponse> alertasDeFraude() {
        return alertas.findTop50ByOrderByCreadoEnDesc().stream().map(AlertaFraudeResponse::from).toList();
    }
}

package com.petshop.orden.service;

import com.petshop.orden.domain.Orden;
import com.petshop.orden.messaging.OrdenCreadaEvent;
import com.petshop.orden.messaging.OrdenPublisher;
import com.petshop.orden.messaging.StockDescontarCommand;
import com.petshop.orden.repository.OrdenRepository;
import com.petshop.orden.web.dto.OrdenRequest;
import com.petshop.orden.web.dto.OrdenResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrdenService {

    private final OrdenRepository repository;
    private final OrdenPublisher publisher;

    public OrdenService(OrdenRepository repository, OrdenPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public List<OrdenResponse> listar(String usuarioId) {
        return repository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream()
                .map(OrdenResponse::from)
                .toList();
    }

    /**
     * Guarda la orden y publica los mensajes asincronos: el correo de confirmacion siempre y,
     * si la peticion trae items, el descuento de stock.
     */
    public OrdenResponse crear(String usuarioId, String email, BigDecimal total, List<OrdenRequest.ItemRequest> items) {
        Orden orden = repository.save(new Orden(usuarioId, total));
        publisher.publicar(new OrdenCreadaEvent(orden.getId(), email, orden.getTotal()));
        if (items != null && !items.isEmpty()) {
            publisher.publicarDescuentoDeStock(new StockDescontarCommand(orden.getId(),
                    items.stream()
                            .map(i -> new StockDescontarCommand.Linea(i.productoId(), i.cantidad()))
                            .toList()));
        }
        return OrdenResponse.from(orden);
    }

    /**
     * Resultado de un pago (cola pago.procesado). APROBADO -> PAGADA; cualquier otro -> PAGO_RECHAZADO,
     * salvo que la orden ya este PAGADA (un intento fallido posterior no la deshace).
     * @return false si la orden no existe.
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean registrarPago(Long ordenId, String estadoPago) {
        return repository.findById(ordenId).map(orden -> {
            if ("APROBADO".equals(estadoPago)) {
                orden.setEstado("PAGADA");
            } else if (!"PAGADA".equals(orden.getEstado())) {
                orden.setEstado("PAGO_RECHAZADO");
            }
            repository.save(orden);
            org.slf4j.LoggerFactory.getLogger(OrdenService.class)
                    .info("Orden {}: pago {} -> estado {}", ordenId, estadoPago, orden.getEstado());
            return true;
        }).orElse(false);
    }
}

package com.petshop.orden.service;

import com.petshop.orden.domain.Orden;
import com.petshop.orden.messaging.OrdenCreadaEvent;
import com.petshop.orden.messaging.OrdenPublisher;
import com.petshop.orden.repository.OrdenRepository;
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

    public OrdenResponse crear(String usuarioId, String email, BigDecimal total) {
        Orden orden = repository.save(new Orden(usuarioId, total));
        publisher.publicar(new OrdenCreadaEvent(orden.getId(), email, orden.getTotal()));
        return OrdenResponse.from(orden);
    }
}

package com.petshop.orden.service;

import com.petshop.orden.domain.Orden;
import com.petshop.orden.repository.OrdenRepository;
import com.petshop.orden.web.dto.OrdenResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrdenService {

    private final OrdenRepository repository;

    public OrdenService(OrdenRepository repository) {
        this.repository = repository;
    }

    public List<OrdenResponse> listar(String usuarioId) {
        return repository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream()
                .map(OrdenResponse::from)
                .toList();
    }

    public OrdenResponse crear(String usuarioId, BigDecimal total) {
        return OrdenResponse.from(repository.save(new Orden(usuarioId, total)));
    }
}

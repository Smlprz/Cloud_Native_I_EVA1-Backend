package com.petshop.orden.repository;

import com.petshop.orden.domain.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);
}

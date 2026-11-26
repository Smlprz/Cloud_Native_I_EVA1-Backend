package com.petshop.productos.repository;

import com.petshop.productos.domain.ResenaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaProcesadaRepository extends JpaRepository<ResenaProcesada, Long> {

    boolean existsByResenaId(Long resenaId);
}

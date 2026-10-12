package com.petshop.resenas.repository;

import com.petshop.resenas.domain.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoIdOrderByCreadoEnDesc(Long productoId);
}

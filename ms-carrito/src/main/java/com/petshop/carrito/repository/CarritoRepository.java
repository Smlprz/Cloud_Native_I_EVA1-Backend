package com.petshop.carrito.repository;

import com.petshop.carrito.domain.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioIdAndEstado(String usuarioId, String estado);
}

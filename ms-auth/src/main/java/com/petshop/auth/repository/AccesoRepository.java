package com.petshop.auth.repository;

import com.petshop.auth.domain.Acceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    Page<Acceso> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId, Pageable pageable);
}

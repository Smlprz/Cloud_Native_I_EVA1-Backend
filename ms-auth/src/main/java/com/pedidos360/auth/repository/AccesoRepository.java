package com.pedidos360.auth.repository;

import com.pedidos360.auth.domain.Acceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    Page<Acceso> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId, Pageable pageable);
}

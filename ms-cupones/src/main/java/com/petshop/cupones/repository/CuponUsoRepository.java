package com.petshop.cupones.repository;

import com.petshop.cupones.domain.CuponUso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuponUsoRepository extends JpaRepository<CuponUso, Long> {

    boolean existsByCuponIdAndUsuarioId(Long cuponId, String usuarioId);
}

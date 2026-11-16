package com.petshop.pago.repository;

import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);

    List<Pago> findByUsuarioIdAndOrdenIdOrderByCreadoEnDesc(String usuarioId, Long ordenId);

    boolean existsByOrdenIdAndEstado(Long ordenId, EstadoPago estado);
}

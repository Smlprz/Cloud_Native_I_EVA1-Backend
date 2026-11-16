package com.petshop.productos.repository;

import com.petshop.productos.domain.StockMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovimientoRepository extends JpaRepository<StockMovimiento, Long> {

    boolean existsByOrdenId(Long ordenId);
}

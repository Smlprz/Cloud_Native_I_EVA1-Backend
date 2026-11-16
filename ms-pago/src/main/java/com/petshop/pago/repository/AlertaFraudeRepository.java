package com.petshop.pago.repository;

import com.petshop.pago.domain.AlertaFraude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, Long> {

    List<AlertaFraude> findTop50ByOrderByCreadoEnDesc();
}

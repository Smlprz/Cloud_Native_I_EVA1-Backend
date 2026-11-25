package com.petshop.pago.repository;

import com.petshop.pago.domain.WebpayTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebpayTransaccionRepository extends JpaRepository<WebpayTransaccion, String> {
}

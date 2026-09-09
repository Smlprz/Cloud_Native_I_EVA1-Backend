package com.pedidos360.carrito.repository;

import com.pedidos360.carrito.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);
}

package com.pedidos360.productos.repository;

import com.pedidos360.productos.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoriaIgnoreCase(String categoria);

    List<Product> findByNombreContainingIgnoreCase(String nombre);
}

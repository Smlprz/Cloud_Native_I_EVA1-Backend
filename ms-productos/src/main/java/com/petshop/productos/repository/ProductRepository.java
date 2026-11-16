package com.petshop.productos.repository;

import com.petshop.productos.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoriaIgnoreCase(String categoria);

    List<Product> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Descuenta stock de forma atomica: solo actua si el producto existe y alcanza el stock.
     * Devuelve las filas afectadas (0 = producto inexistente o stock insuficiente).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}

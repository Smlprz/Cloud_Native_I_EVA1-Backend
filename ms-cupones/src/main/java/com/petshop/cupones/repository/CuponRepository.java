package com.petshop.cupones.repository;

import com.petshop.cupones.domain.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {

    Optional<Cupon> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Cupon> findAllByOrderByCreadoEnDesc();

    /**
     * Consume un uso de forma atomica en la base de datos: la condicion y el incremento van en la misma sentencia,
     * asi que con muchas solicitudes simultaneas nunca se pasa de usosMaximos. Devuelve 0 si ya no quedan usos.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Cupon c SET c.usosActuales = c.usosActuales + 1 "
            + "WHERE c.id = :id AND c.activo = true AND (c.usosMaximos IS NULL OR c.usosActuales < c.usosMaximos)")
    int consumirUso(@Param("id") Long id);
}

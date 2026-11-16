package com.petshop.productos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Marca de que el stock de una orden ya fue descontado (garantiza idempotencia). */
@Entity
@Table(name = "stock_movimientos")
public class StockMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, unique = true)
    private Long ordenId;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    protected StockMovimiento() {
    }

    public StockMovimiento(Long ordenId) {
        this.ordenId = ordenId;
    }

    public Long getId() {
        return id;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}

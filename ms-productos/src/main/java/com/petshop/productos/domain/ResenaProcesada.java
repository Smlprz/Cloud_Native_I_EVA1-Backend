package com.petshop.productos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Marca de que una resena ya se sumo a la calificacion del producto (garantiza idempotencia). */
@Entity
@Table(name = "resenas_procesadas")
public class ResenaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resena_id", nullable = false, unique = true)
    private Long resenaId;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    protected ResenaProcesada() {
    }

    public ResenaProcesada(Long resenaId) {
        this.resenaId = resenaId;
    }

    public Long getResenaId() {
        return resenaId;
    }
}

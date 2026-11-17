package com.petshop.cupones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro de que un usuario uso un cupon. La restriccion unica (cupon, usuario) impide usarlo dos veces. */
@Entity
@Table(name = "cupon_usos", uniqueConstraints = @UniqueConstraint(name = "uk_uso_usuario", columnNames = {"cupon_id", "usuario_id"}))
public class CuponUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cupon_id", nullable = false)
    private Long cuponId;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "monto_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoOriginal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected CuponUso() {
    }

    public CuponUso(Long cuponId, String usuarioId, BigDecimal montoOriginal, BigDecimal descuento) {
        this.cuponId = cuponId;
        this.usuarioId = usuarioId;
        this.montoOriginal = montoOriginal;
        this.descuento = descuento;
    }

    public Long getId() { return id; }
    public Long getCuponId() { return cuponId; }
    public String getUsuarioId() { return usuarioId; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public BigDecimal getDescuento() { return descuento; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

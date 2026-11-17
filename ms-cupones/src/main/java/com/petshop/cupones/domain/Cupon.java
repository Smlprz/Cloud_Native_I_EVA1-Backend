package com.petshop.cupones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Cupon de descuento. El codigo se guarda siempre en mayusculas. */
@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCupon tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "monto_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMinimo = BigDecimal.ZERO;

    @Column(name = "vence_en")
    private LocalDateTime venceEn;

    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    @Column(name = "usos_actuales", nullable = false)
    private int usosActuales;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Cupon() {
    }

    public Cupon(String codigo, TipoCupon tipo, BigDecimal valor, BigDecimal montoMinimo,
                 LocalDateTime venceEn, Integer usosMaximos) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.valor = valor;
        this.montoMinimo = montoMinimo != null ? montoMinimo : BigDecimal.ZERO;
        this.venceEn = venceEn;
        this.usosMaximos = usosMaximos;
    }

    /** Descuento sobre un monto: nunca supera el monto (el total no queda negativo). Redondeo a 2 decimales. */
    public BigDecimal descuentoSobre(BigDecimal monto) {
        BigDecimal descuento = tipo == TipoCupon.PORCENTAJE
                ? monto.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : valor;
        return descuento.min(monto).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean estaVencido(LocalDateTime ahora) {
        return venceEn != null && venceEn.isBefore(ahora);
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoCupon getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public Integer getUsosMaximos() { return usosMaximos; }
    public int getUsosActuales() { return usosActuales; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

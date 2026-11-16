package com.petshop.pago.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Intento de pago de una orden. Guarda solo los ultimos 4 digitos de la tarjeta y su hash:
 * el numero completo y el CVV no se almacenan nunca.
 */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "tarjeta_ultimos4", nullable = false, length = 4)
    private String tarjetaUltimos4;

    @Column(name = "tarjeta_hash", nullable = false, length = 64)
    private String tarjetaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado;

    @Column(length = 255)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Pago() {
    }

    public Pago(Long ordenId, String usuarioId, BigDecimal monto, String tarjetaUltimos4,
                String tarjetaHash, EstadoPago estado, String motivo) {
        this.ordenId = ordenId;
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.tarjetaUltimos4 = tarjetaUltimos4;
        this.tarjetaHash = tarjetaHash;
        this.estado = estado;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public String getUsuarioId() { return usuarioId; }
    public BigDecimal getMonto() { return monto; }
    public String getTarjetaUltimos4() { return tarjetaUltimos4; }
    public String getTarjetaHash() { return tarjetaHash; }
    public EstadoPago getEstado() { return estado; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

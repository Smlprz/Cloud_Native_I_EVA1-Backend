package com.petshop.pago.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Transaccion iniciada en Webpay: relaciona el token de Transbank con la orden y el usuario que la inicio. */
@Entity
@Table(name = "webpay_transacciones")
public class WebpayTransaccion {

    @Id
    @Column(length = 100)
    private String token;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "buy_order", nullable = false, length = 26)
    private String buyOrder;

    /** Id del pago registrado al terminar; null mientras el usuario esta en Webpay. */
    @Column(name = "pago_id")
    private Long pagoId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected WebpayTransaccion() {
    }

    public WebpayTransaccion(String token, Long ordenId, String usuarioId, BigDecimal monto, String buyOrder) {
        this.token = token;
        this.ordenId = ordenId;
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.buyOrder = buyOrder;
    }

    public void finalizar(Long pagoId) {
        this.pagoId = pagoId;
    }

    public String getToken() { return token; }
    public Long getOrdenId() { return ordenId; }
    public String getUsuarioId() { return usuarioId; }
    public BigDecimal getMonto() { return monto; }
    public String getBuyOrder() { return buyOrder; }
    public Long getPagoId() { return pagoId; }
}

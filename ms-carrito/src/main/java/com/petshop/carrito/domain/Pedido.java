package com.petshop.carrito.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido confirmado: copia inmutable ("snapshot") del carrito al momento del checkout.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    /** Descuento aplicado por un cupon (0 si no se uso). El total ya viene con el descuento restado. */
    @Column(name = "descuento", nullable = false, precision = 15, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "cupon_codigo", length = 40)
    private String cuponCodigo;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "CONFIRMADO";

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    protected Pedido() {
    }

    public Pedido(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        this.items.add(item);
    }

    public void recalcularTotal() {
        this.total = items.stream()
                .map(PedidoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Resta el descuento de un cupon al total ya calculado; el total nunca queda negativo. */
    public void aplicarDescuento(BigDecimal descuento, String cuponCodigo) {
        this.descuento = descuento;
        this.cuponCodigo = cuponCodigo;
        this.total = this.total.subtract(descuento).max(BigDecimal.ZERO);
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public String getCuponCodigo() {
        return cuponCodigo;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public List<PedidoItem> getItems() {
        return items;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}

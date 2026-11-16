package com.petshop.pago.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Alerta registrada cuando la regla antifraude bloquea una transaccion. */
@Entity
@Table(name = "alertas_fraude")
public class AlertaFraude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "tarjeta_ultimos4", nullable = false, length = 4)
    private String tarjetaUltimos4;

    @Column(name = "tarjeta_hash", nullable = false, length = 64)
    private String tarjetaHash;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ventana_segundos", nullable = false)
    private int ventanaSegundos;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected AlertaFraude() {
    }

    public AlertaFraude(String usuarioId, Long ordenId, String tarjetaUltimos4, String tarjetaHash,
                        int intentos, int ventanaSegundos) {
        this.usuarioId = usuarioId;
        this.ordenId = ordenId;
        this.tarjetaUltimos4 = tarjetaUltimos4;
        this.tarjetaHash = tarjetaHash;
        this.intentos = intentos;
        this.ventanaSegundos = ventanaSegundos;
    }

    public Long getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public Long getOrdenId() { return ordenId; }
    public String getTarjetaUltimos4() { return tarjetaUltimos4; }
    public String getTarjetaHash() { return tarjetaHash; }
    public int getIntentos() { return intentos; }
    public int getVentanaSegundos() { return ventanaSegundos; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

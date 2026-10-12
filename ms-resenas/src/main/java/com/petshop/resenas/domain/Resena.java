package com.petshop.resenas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Opinion de un cliente sobre un producto (calificacion de 1 a 5 y comentario). */
@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(nullable = false)
    private int puntaje;

    @Column(length = 1000)
    private String comentario;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Resena() {
    }

    public Resena(Long productoId, String usuarioId, String usuarioNombre, int puntaje, String comentario) {
        this.productoId = productoId;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.puntaje = puntaje;
        this.comentario = comentario;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getUsuarioId() { return usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public int getPuntaje() { return puntaje; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

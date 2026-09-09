package com.pedidos360.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Registro de auditoria: un acceso (consulta de perfil) del usuario autenticado. */
@Entity
@Table(name = "accesos")
public class Acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "nombre", length = 150)
    private String nombre;

    @Column(name = "email", length = 150)
    private String email;

    /** Roles del token, separados por coma (ej: "ADMIN,CLIENTE"). */
    @Column(name = "roles", length = 200)
    private String roles;

    @Column(name = "ip", length = 45)
    private String ip;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    protected Acceso() {
    }

    public Acceso(String usuarioId, String nombre, String email, String roles, String ip) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.email = email;
        this.roles = roles;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getRoles() {
        return roles;
    }

    public String getIp() {
        return ip;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}

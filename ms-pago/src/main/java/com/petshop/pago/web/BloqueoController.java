package com.petshop.pago.web;

import com.petshop.pago.antifraude.Antifraude;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Bloqueos del antifraude: el usuario consulta el suyo; un ADMIN puede desbloquear a cualquiera. */
@RestController
@RequestMapping("/api/pagos")
public class BloqueoController {

    private final Antifraude antifraude;

    public BloqueoController(Antifraude antifraude) {
        this.antifraude = antifraude;
    }

    @GetMapping("/bloqueo")
    @PreAuthorize("hasAnyAuthority('SCOPE_pagos.read','SCOPE_ordenes.read','SCOPE_carrito.read') and hasAnyRole('CLIENTE','ADMIN')")
    public BloqueoResponse miBloqueo(@AuthenticationPrincipal Jwt jwt) {
        Instant hasta = antifraude.bloqueadoHasta(UsuarioActual.id(jwt)).orElse(null);
        return new BloqueoResponse(hasta != null, hasta);
    }

    @DeleteMapping("/bloqueos/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_pagos.write','SCOPE_ordenes.write','SCOPE_carrito.write') and hasRole('ADMIN')")
    public ResponseEntity<Void> desbloquear(@PathVariable String usuarioId) {
        antifraude.desbloquear(usuarioId);
        return ResponseEntity.noContent().build();
    }

    public record BloqueoResponse(boolean bloqueado, Instant hasta) {
    }
}

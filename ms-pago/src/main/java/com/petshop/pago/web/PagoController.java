package com.petshop.pago.web;

import com.petshop.pago.service.PagoService;
import com.petshop.pago.web.dto.AlertaFraudeResponse;
import com.petshop.pago.web.dto.PagoRequest;
import com.petshop.pago.web.dto.PagoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de pagos. Exige JWT de Azure AD con scope pagos.* (o ordenes.* / carrito.* mientras pagos.* no exista en
 * el App Registration) y rol CLIENTE o ADMIN. Las alertas de fraude solo las ve un ADMIN.
 *
 * Codigos: 201 pago aprobado, 402 rechazado (tarjeta invalida, vencida, sin fondos),
 * 429 bloqueado por el antifraude, 404 orden inexistente, 409 orden ya pagada.
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_pagos.write','SCOPE_ordenes.write','SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public ResponseEntity<?> pagar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PagoRequest request) {
        PagoResponse pago = service.procesar(UsuarioActual.id(jwt), jwt.getTokenValue(), request);
        return switch (pago.estado()) {
            case APROBADO -> ResponseEntity.status(HttpStatus.CREATED).body(pago);
            case RECHAZADO -> ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problema(HttpStatus.PAYMENT_REQUIRED, pago));
            case BLOQUEADO_FRAUDE -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problema(HttpStatus.TOO_MANY_REQUESTS, pago));
        };
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_pagos.read','SCOPE_ordenes.read','SCOPE_carrito.read') and hasAnyRole('CLIENTE','ADMIN')")
    public List<PagoResponse> misPagos(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long ordenId) {
        return service.listar(UsuarioActual.id(jwt), ordenId);
    }

    @GetMapping("/alertas-fraude")
    @PreAuthorize("hasAnyAuthority('SCOPE_pagos.read','SCOPE_ordenes.read','SCOPE_carrito.read') and hasRole('ADMIN')")
    public List<AlertaFraudeResponse> alertasDeFraude() {
        return service.alertasDeFraude();
    }

    /** El rechazo se devuelve como ProblemDetail (campo "detail" = motivo) para que el cliente muestre el mensaje. */
    private static ProblemDetail problema(HttpStatus status, PagoResponse pago) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, pago.motivo());
        problema.setProperty("estado", pago.estado());
        problema.setProperty("pagoId", pago.id());
        problema.setProperty("ordenId", pago.ordenId());
        return problema;
    }
}

package com.petshop.pago.web;

import com.petshop.pago.service.WebpayService;
import com.petshop.pago.web.dto.PagoResponse;
import com.petshop.pago.web.dto.WebpayInicioResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pago con Webpay Plus. 429 = bloqueado por el antifraude (por id de usuario). */
@RestController
@RequestMapping("/api/pagos/webpay")
public class WebpayController {

    private static final String PUEDE_PAGAR =
            "hasAnyAuthority('SCOPE_pagos.write','SCOPE_ordenes.write','SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')";

    private final WebpayService service;

    public WebpayController(WebpayService service) {
        this.service = service;
    }

    public record IniciarRequest(@NotNull @Positive Long ordenId) {
    }

    public record TokenRequest(@NotBlank @Size(max = 100) String token) {
    }

    @PostMapping("/iniciar")
    @PreAuthorize(PUEDE_PAGAR)
    public WebpayInicioResponse iniciar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IniciarRequest request) {
        return service.iniciar(UsuarioActual.id(jwt), jwt.getTokenValue(), request.ordenId());
    }

    @PostMapping("/confirmar")
    @PreAuthorize(PUEDE_PAGAR)
    public PagoResponse confirmar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TokenRequest request) {
        return service.confirmar(UsuarioActual.id(jwt), request.token());
    }

    @PostMapping("/anular")
    @PreAuthorize(PUEDE_PAGAR)
    public PagoResponse anular(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TokenRequest request) {
        return service.anular(UsuarioActual.id(jwt), request.token());
    }
}

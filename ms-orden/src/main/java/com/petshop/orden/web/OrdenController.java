package com.petshop.orden.web;

import com.petshop.orden.service.OrdenService;
import com.petshop.orden.web.dto.OrdenRequest;
import com.petshop.orden.web.dto.OrdenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API privada de ordenes. Exige JWT de Azure AD (app pedidos360-api) con scope ordenes.read / ordenes.write,
 * o los equivalentes carrito.read / carrito.write, para que ms-carrito reenvie el token del usuario.
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_ordenes.read','SCOPE_carrito.read') and hasAnyRole('CLIENTE','ADMIN')")
    public List<OrdenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(UsuarioActual.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SCOPE_ordenes.write','SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public OrdenResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrdenRequest request) {
        return service.crear(UsuarioActual.id(jwt), UsuarioActual.email(jwt), request.total());
    }
}

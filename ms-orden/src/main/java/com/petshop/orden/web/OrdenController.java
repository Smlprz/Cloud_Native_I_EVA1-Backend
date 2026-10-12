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
 * API privada de ordenes. Exige JWT de Azure AD con scope ordenes.read / ordenes.write
 * (estos scopes hay que crearlos en el App Registration de la API).
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ordenes.read') and hasAnyRole('CLIENTE','ADMIN')")
    public List<OrdenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(UsuarioActual.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_ordenes.write') and hasAnyRole('CLIENTE','ADMIN')")
    public OrdenResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrdenRequest request) {
        return service.crear(UsuarioActual.id(jwt), request.total());
    }
}

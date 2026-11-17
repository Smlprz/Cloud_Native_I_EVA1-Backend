package com.petshop.cupones.web;

import com.petshop.cupones.messaging.CuponEventPublisher;
import com.petshop.cupones.service.CuponService;
import com.petshop.cupones.web.dto.ActivoRequest;
import com.petshop.cupones.web.dto.CalculoResponse;
import com.petshop.cupones.web.dto.CuponRequest;
import com.petshop.cupones.web.dto.CuponResponse;
import com.petshop.cupones.web.dto.UsoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de cupones. Exige JWT de Azure AD. Scopes: cupones.* (o carrito.* mientras cupones.* no exista en el
 * App Registration).
 *  - Clientes: POST /validar (calcula el descuento sin consumir) y POST /aplicar (lo consume; lo llama ms-carrito
 *    en el checkout reenviando el JWT del usuario).
 *  - Administradores: crear, listar y activar/desactivar cupones.
 */
@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private static final String LEER = "hasAnyAuthority('SCOPE_cupones.read','SCOPE_carrito.read','SCOPE_carrito.write')";
    private static final String ESCRIBIR = "hasAnyAuthority('SCOPE_cupones.write','SCOPE_carrito.write')";
    private static final String ADMIN = "hasRole('ADMIN') and hasAnyAuthority('SCOPE_cupones.admin','SCOPE_carrito.write')";

    private final CuponService service;
    private final CuponEventPublisher eventos;

    public CuponController(CuponService service, CuponEventPublisher eventos) {
        this.service = service;
        this.eventos = eventos;
    }

    @PostMapping("/validar")
    @PreAuthorize(LEER + " and hasAnyRole('CLIENTE','ADMIN')")
    public CalculoResponse validar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsoRequest request) {
        return service.validar(UsuarioActual.id(jwt), request.codigo(), request.monto());
    }

    @PostMapping("/aplicar")
    @PreAuthorize(ESCRIBIR + " and hasAnyRole('CLIENTE','ADMIN')")
    public CalculoResponse aplicar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsoRequest request) {
        String usuario = UsuarioActual.id(jwt);
        CalculoResponse resultado = service.aplicar(usuario, request.codigo(), request.monto());
        eventos.cuponAplicado(usuario, resultado.codigo(), resultado.descuento()); // ya confirmado en la base
        return resultado;
    }

    @GetMapping
    @PreAuthorize(ADMIN)
    public List<CuponResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public CuponResponse crear(@Valid @RequestBody CuponRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}/activo")
    @PreAuthorize(ADMIN)
    public CuponResponse cambiarActivo(@PathVariable Long id, @Valid @RequestBody ActivoRequest request) {
        return service.cambiarActivo(id, request.activo());
    }
}

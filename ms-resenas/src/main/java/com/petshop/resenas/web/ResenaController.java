package com.petshop.resenas.web;

import com.petshop.resenas.service.ResenaService;
import com.petshop.resenas.web.dto.ResenaRequest;
import com.petshop.resenas.web.dto.ResenaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API privada de resenas. Exige JWT de Azure AD con scope resenas.read / resenas.write
 * (estos scopes hay que crearlos en el App Registration de la API).
 */
@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaService service;

    public ResenaController(ResenaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_resenas.read')")
    public List<ResenaResponse> listar(@RequestParam Long productoId) {
        return service.listarPorProducto(productoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_resenas.write') and hasAnyRole('CLIENTE','ADMIN')")
    public ResenaResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ResenaRequest request) {
        return service.crear(UsuarioActual.id(jwt), UsuarioActual.nombre(jwt), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_resenas.write') and hasAnyRole('CLIENTE','ADMIN')")
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.eliminar(id, UsuarioActual.id(jwt));
    }
}

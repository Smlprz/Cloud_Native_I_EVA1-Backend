package com.pedidos360.carrito.web;

import com.pedidos360.carrito.service.CarritoService;
import com.pedidos360.carrito.web.dto.ActualizarItemRequest;
import com.pedidos360.carrito.web.dto.AgregarItemRequest;
import com.pedidos360.carrito.web.dto.CarritoResponse;
import com.pedidos360.carrito.web.dto.PedidoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * API privada del carrito. Todo endpoint exige un JWT valido de Azure AD con:
 *  - scope  carrito.read  para lectura,  carrito.write  para modificar (claim "scp").
 *  - rol    CLIENTE  o  ADMIN  (del claim "roles" si existe, o resuelto en el backend
 *           por email; ver AzureJwtConverter).
 * El AWS API Gateway valida el token en el borde y este servicio lo revalida.
 */
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService service;

    public CarritoController(CarritoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_carrito.read') and hasAnyRole('CLIENTE','ADMIN')")
    public CarritoResponse verCarrito(@AuthenticationPrincipal Jwt jwt) {
        return service.verCarrito(UsuarioActual.id(jwt));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public CarritoResponse agregarItem(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AgregarItemRequest request) {
        return service.agregarItem(
                UsuarioActual.id(jwt), request.productoId(), request.cantidad(), jwt.getTokenValue());
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public CarritoResponse actualizarItem(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long itemId,
                                          @Valid @RequestBody ActualizarItemRequest request) {
        return service.actualizarItem(UsuarioActual.id(jwt), itemId, request.cantidad());
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public CarritoResponse eliminarItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return service.eliminarItem(UsuarioActual.id(jwt), itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public void vaciar(@AuthenticationPrincipal Jwt jwt) {
        service.vaciar(UsuarioActual.id(jwt));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_carrito.write') and hasAnyRole('CLIENTE','ADMIN')")
    public PedidoResponse checkout(@AuthenticationPrincipal Jwt jwt) {
        return service.checkout(UsuarioActual.id(jwt));
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasAuthority('SCOPE_carrito.read') and hasAnyRole('CLIENTE','ADMIN')")
    public List<PedidoResponse> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        return service.listarPedidos(UsuarioActual.id(jwt));
    }
}

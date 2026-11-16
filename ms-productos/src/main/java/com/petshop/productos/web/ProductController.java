package com.petshop.productos.web;

import com.petshop.productos.messaging.kafka.NavegacionPublisher;
import com.petshop.productos.service.ProductService;
import com.petshop.productos.web.dto.ProductRequest;
import com.petshop.productos.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API del catalogo. Se expone hacia el exterior a traves del AWS API Gateway.
 * - Los GET son publicos (catalogo visible sin sesion).
 * - Las escrituras exigen un JWT valido de Azure AD con el scope productos.write
 *   y rol VENDEDOR/ADMIN (DELETE solo ADMIN). El API Gateway ademas valida el token en el borde.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductController {

    private final ProductService service;
    private final NavegacionPublisher navegacion;

    public ProductController(ProductService service, NavegacionPublisher navegacion) {
        this.service = service;
        this.navegacion = navegacion;
    }

    @GetMapping
    public List<ProductResponse> listar(@RequestParam(required = false) String categoria,
                                        @RequestParam(required = false) String nombre) {
        List<com.petshop.productos.domain.Product> productos;
        if (categoria != null && !categoria.isBlank()) {
            productos = service.listarPorCategoria(categoria);
        } else if (nombre != null && !nombre.isBlank()) {
            productos = service.buscarPorNombre(nombre);
        } else {
            productos = service.listar();
        }
        return productos.stream().map(ProductResponse::from).toList();
    }

    /**
     * Detalle de un producto (publico). Cada visita se publica en Kafka (eventos.navegacion) para el analisis de
     * navegacion y las recomendaciones; el usuario sale del JWT si lo hay, y si no queda como "anonimo".
     */
    @GetMapping("/{id}")
    public ProductResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        com.petshop.productos.domain.Product producto = service.obtener(id);
        navegacion.productoVisto(usuarioDe(jwt), producto);
        return ProductResponse.from(producto);
    }

    private static String usuarioDe(Jwt jwt) {
        if (jwt == null) {
            return "anonimo";
        }
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_productos.write') and hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<ProductResponse> crear(@Valid @RequestBody ProductRequest request) {
        ProductResponse creado = ProductResponse.from(service.crear(request));
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_productos.write') and hasAnyRole('VENDEDOR','ADMIN')")
    public ProductResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_productos.write') and hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

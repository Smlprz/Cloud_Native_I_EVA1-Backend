package com.pedidos360.productos.web;

import com.pedidos360.productos.service.ProductService;
import com.pedidos360.productos.web.dto.ProductRequest;
import com.pedidos360.productos.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> listar(@RequestParam(required = false) String categoria,
                                        @RequestParam(required = false) String nombre) {
        List<com.pedidos360.productos.domain.Product> productos;
        if (categoria != null && !categoria.isBlank()) {
            productos = service.listarPorCategoria(categoria);
        } else if (nombre != null && !nombre.isBlank()) {
            productos = service.buscarPorNombre(nombre);
        } else {
            productos = service.listar();
        }
        return productos.stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse obtener(@PathVariable Long id) {
        return ProductResponse.from(service.obtener(id));
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

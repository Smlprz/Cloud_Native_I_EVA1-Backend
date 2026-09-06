package com.pedidos360.productos.service;

import com.pedidos360.productos.domain.Product;
import com.pedidos360.productos.repository.ProductRepository;
import com.pedidos360.productos.web.dto.ProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> listarPorCategoria(String categoria) {
        return repository.findByCategoriaIgnoreCase(categoria);
    }

    @Transactional(readOnly = true)
    public List<Product> buscarPorNombre(String nombre) {
        return repository.findByNombreContainingIgnoreCase(nombre);
    }

    @Transactional(readOnly = true)
    public Product obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto " + id + " no encontrado"));
    }

    public Product crear(ProductRequest req) {
        Product producto = new Product(
                req.nombre(),
                req.descripcion(),
                req.precio(),
                req.stock(),
                req.categoria(),
                req.imagenUrl(),
                req.idVendedor()
        );
        return repository.save(producto);
    }

    public Product actualizar(Long id, ProductRequest req) {
        Product producto = obtener(id);
        producto.setNombre(req.nombre());
        producto.setDescripcion(req.descripcion());
        producto.setPrecio(req.precio());
        producto.setStock(req.stock());
        producto.setCategoria(req.categoria());
        producto.setImagenUrl(req.imagenUrl());
        producto.setIdVendedor(req.idVendedor());
        return repository.save(producto);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Producto " + id + " no encontrado");
        }
        repository.deleteById(id);
    }
}

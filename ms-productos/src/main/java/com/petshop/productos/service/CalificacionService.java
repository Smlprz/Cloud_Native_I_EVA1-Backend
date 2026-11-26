package com.petshop.productos.service;

import com.petshop.productos.domain.Product;
import com.petshop.productos.domain.ResenaProcesada;
import com.petshop.productos.repository.ProductRepository;
import com.petshop.productos.repository.ResenaProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suma cada resena nueva a la calificacion promedio del producto.
 * Es idempotente por resenaId: un mensaje duplicado no se cuenta dos veces.
 */
@Service
@Transactional
public class CalificacionService {

    private static final Logger log = LoggerFactory.getLogger(CalificacionService.class);

    private final ProductRepository productRepository;
    private final ResenaProcesadaRepository procesadas;

    public CalificacionService(ProductRepository productRepository, ResenaProcesadaRepository procesadas) {
        this.productRepository = productRepository;
        this.procesadas = procesadas;
    }

    /** @return false si la resena ya estaba contada. */
    public boolean registrar(Long resenaId, Long productoId, int puntaje) {
        if (procesadas.existsByResenaId(resenaId)) {
            log.info("Resena {} ya contada: mensaje duplicado, se ignora", resenaId);
            return false;
        }
        Product producto = productRepository.findById(productoId)
                .orElseThrow(() -> new ProductoInexistenteException("Producto " + productoId + " inexistente"));
        producto.agregarCalificacion(puntaje);
        procesadas.save(new ResenaProcesada(resenaId));
        log.info("Producto {}: calificacion {} con {} resena(s)", productoId,
                producto.getCalificacionPromedio(), producto.getResenasCantidad());
        return true;
    }
}

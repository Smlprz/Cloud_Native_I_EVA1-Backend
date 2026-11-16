package com.petshop.productos.service;

import com.petshop.productos.domain.Product;
import com.petshop.productos.domain.StockMovimiento;
import com.petshop.productos.repository.ProductRepository;
import com.petshop.productos.repository.StockMovimientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Descuenta el stock de una orden. Todo o nada: si una linea no tiene stock suficiente se
 * lanza {@link StockInsuficienteException} y la transaccion revierte los descuentos previos.
 * Es idempotente por ordenId: una entrega duplicada del mensaje no descuenta dos veces.
 */
@Service
@Transactional
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final StockMovimientoRepository movimientoRepository;
    private final int umbralBajo;

    public StockService(ProductRepository productRepository,
                        StockMovimientoRepository movimientoRepository,
                        @Value("${app.stock.umbral-bajo:5}") int umbralBajo) {
        this.productRepository = productRepository;
        this.movimientoRepository = movimientoRepository;
        this.umbralBajo = umbralBajo;
    }

    public record Linea(Long productoId, int cantidad) {
    }

    /** Producto que quedo con stock igual o menor al umbral tras un descuento. */
    public record StockBajo(Long productoId, String nombre, int stock, int umbral) {
    }

    /** @return los productos que quedaron con stock bajo (vacio si el mensaje era un duplicado). */
    public List<StockBajo> descontar(Long ordenId, List<Linea> lineas) {
        if (movimientoRepository.existsByOrdenId(ordenId)) {
            log.info("Stock de la orden {} ya descontado: mensaje duplicado, se ignora", ordenId);
            return List.of();
        }
        for (Linea linea : lineas) {
            int filas = productRepository.descontarStock(linea.productoId(), linea.cantidad());
            if (filas == 0) {
                throw new StockInsuficienteException("Producto " + linea.productoId()
                        + " inexistente o sin stock suficiente para " + linea.cantidad() + " unidad(es)");
            }
        }
        movimientoRepository.save(new StockMovimiento(ordenId));
        log.info("Stock descontado para la orden {} ({} linea(s))", ordenId, lineas.size());

        List<Long> ids = lineas.stream().map(Linea::productoId).distinct().toList();
        return productRepository.findAllById(ids).stream()
                .filter(p -> p.getStock() <= umbralBajo)
                .map(this::aStockBajo)
                .toList();
    }

    private StockBajo aStockBajo(Product p) {
        return new StockBajo(p.getId(), p.getNombre(), p.getStock(), umbralBajo);
    }
}

package com.petshop.productos.service;

import com.petshop.productos.domain.Product;
import com.petshop.productos.repository.ProductRepository;
import com.petshop.productos.repository.StockMovimientoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StockServiceTest {

    @Autowired
    private StockService service;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovimientoRepository movimientoRepository;

    private Product producto(String nombre, int stock) {
        return productRepository.save(new Product(nombre, "desc", new BigDecimal("1000.00"), stock,
                "Test", "img.webp", 1L));
    }

    private int stockDe(Product p) {
        return productRepository.findById(p.getId()).orElseThrow().getStock();
    }

    @Test
    void descuentaElStockDeCadaLinea() {
        Product a = producto("Producto A", 20);
        Product b = producto("Producto B", 15);

        service.descontar(9001L, List.of(
                new StockService.Linea(a.getId(), 3), new StockService.Linea(b.getId(), 5)));

        assertThat(stockDe(a)).isEqualTo(17);
        assertThat(stockDe(b)).isEqualTo(10);
        assertThat(movimientoRepository.existsByOrdenId(9001L)).isTrue();
    }

    @Test
    void esIdempotenteAnteElMismoMensajeDuplicado() {
        Product a = producto("Producto C", 20);
        List<StockService.Linea> lineas = List.of(new StockService.Linea(a.getId(), 4));

        service.descontar(9002L, lineas);
        List<StockService.StockBajo> segunda = service.descontar(9002L, lineas);

        assertThat(stockDe(a)).isEqualTo(16);
        assertThat(segunda).isEmpty();
    }

    @Test
    void stockInsuficienteRevierteTodasLasLineasDeLaOrden() {
        Product suficiente = producto("Producto D", 20);
        Product escaso = producto("Producto E", 2);

        assertThatThrownBy(() -> service.descontar(9003L, List.of(
                new StockService.Linea(suficiente.getId(), 4), new StockService.Linea(escaso.getId(), 5))))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(stockDe(suficiente)).isEqualTo(20);
        assertThat(stockDe(escaso)).isEqualTo(2);
        assertThat(movimientoRepository.existsByOrdenId(9003L)).isFalse();
    }

    @Test
    void productoInexistenteEsStockInsuficiente() {
        assertThatThrownBy(() -> service.descontar(9004L, List.of(new StockService.Linea(987654L, 1))))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("987654");
    }

    @Test
    void informaLosProductosQueQuedanConStockBajo() {
        Product casiAgotado = producto("Producto F", 7);   // 7 - 3 = 4  (<= umbral 5)
        Product conHolgura = producto("Producto G", 50);   // 50 - 3 = 47

        List<StockService.StockBajo> bajos = service.descontar(9005L, List.of(
                new StockService.Linea(casiAgotado.getId(), 3), new StockService.Linea(conHolgura.getId(), 3)));

        assertThat(bajos).hasSize(1);
        assertThat(bajos.get(0).productoId()).isEqualTo(casiAgotado.getId());
        assertThat(bajos.get(0).stock()).isEqualTo(4);
        assertThat(bajos.get(0).umbral()).isEqualTo(5);
    }
}

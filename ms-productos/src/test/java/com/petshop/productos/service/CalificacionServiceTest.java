package com.petshop.productos.service;

import com.petshop.productos.domain.Product;
import com.petshop.productos.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CalificacionServiceTest {

    @Autowired
    private CalificacionService service;

    @Autowired
    private ProductRepository productRepository;

    private Product producto() {
        return productRepository.save(new Product("Producto R", "desc", new BigDecimal("1000.00"), 10,
                "Test", "img.webp", 1L));
    }

    @Test
    void calculaElPromedioDeLasResenas() {
        Product p = producto();

        service.registrar(7001L, p.getId(), 5);
        service.registrar(7002L, p.getId(), 4);

        Product actualizado = productRepository.findById(p.getId()).orElseThrow();
        assertThat(actualizado.getResenasCantidad()).isEqualTo(2);
        assertThat(actualizado.getCalificacionPromedio()).isEqualByComparingTo("4.50");
    }

    @Test
    void esIdempotenteAnteLaMismaResenaDuplicada() {
        Product p = producto();

        assertThat(service.registrar(7003L, p.getId(), 3)).isTrue();
        assertThat(service.registrar(7003L, p.getId(), 3)).isFalse();

        assertThat(productRepository.findById(p.getId()).orElseThrow().getResenasCantidad()).isEqualTo(1);
    }

    @Test
    void productoInexistenteEsErrorDeNegocio() {
        assertThatThrownBy(() -> service.registrar(7004L, 999_999L, 5))
                .isInstanceOf(ProductoInexistenteException.class);
    }
}

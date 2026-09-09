package com.pedidos360.productos.repository;

import com.pedidos360.productos.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void guardaYRecuperaPorCategoria() {
        repository.save(new Product("Rascador Gato 137cm", "Rascador con sisal", new BigDecimal("39990.00"),
                12, "Juguetes", "RascadorGato137cm.webp", 3L));
        repository.save(new Product("Salsa Perro 300gr", "Snack", new BigDecimal("2490.00"),
                100, "Snacks", "SalsaPerro300gr.webp", 3L));

        List<Product> juguetes = repository.findByCategoriaIgnoreCase("juguetes");

        assertThat(juguetes).hasSize(1);
        assertThat(juguetes.get(0).getNombre()).isEqualTo("Rascador Gato 137cm");
    }

    @Test
    void buscaPorNombreParcialSinImportarMayusculas() {
        repository.save(new Product("Alimento Perro Adulto Champion", "Seco", new BigDecimal("18990.00"),
                60, "Alimento Perros", "AlimentoPerroAdultoChampion.webp", 1L));

        assertThat(repository.findByNombreContainingIgnoreCase("champion")).hasSize(1);
        assertThat(repository.findByNombreContainingIgnoreCase("gato")).isEmpty();
    }
}

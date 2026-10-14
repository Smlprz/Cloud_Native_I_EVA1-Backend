package com.petshop.orden;

import com.petshop.orden.domain.Orden;
import com.petshop.orden.repository.OrdenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Ejecuta las migraciones Flyway REALES (no el esquema autogenerado de los otros tests) y deja
 * que Hibernate valide que la entidad coincide con las tablas. Si una migracion queda vacia o
 * incompleta, este test falla: es justo el error que dejo sin tabla "ordenes" en produccion.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:orden_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class MigracionesTest {

    @Autowired
    private OrdenRepository repository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void lasMigracionesCreanLaTablaYElEsquemaCoincideConLaEntidad() {
        repository.save(new Orden("usuario-1", new BigDecimal("1500.50")));

        var ordenes = repository.findByUsuarioIdOrderByCreadoEnDesc("usuario-1");

        assertFalse(ordenes.isEmpty());
        assertEquals(new BigDecimal("1500.50"), ordenes.get(0).getTotal());
        assertEquals("CREADA", ordenes.get(0).getEstado());
    }
}

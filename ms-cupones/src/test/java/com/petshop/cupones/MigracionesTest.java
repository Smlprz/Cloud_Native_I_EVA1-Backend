package com.petshop.cupones;

import com.petshop.cupones.domain.Cupon;
import com.petshop.cupones.domain.CuponUso;
import com.petshop.cupones.domain.TipoCupon;
import com.petshop.cupones.messaging.CuponEventPublisher;
import com.petshop.cupones.repository.CuponRepository;
import com.petshop.cupones.repository.CuponUsoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ejecuta las migraciones Flyway REALES y deja que Hibernate valide que las entidades coinciden con las tablas.
 * Comprueba tambien que las restricciones unicas existen: son las que impiden el doble uso aunque fallen las validaciones.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:cupones_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class MigracionesTest {

    @Autowired private CuponRepository cupones;
    @Autowired private CuponUsoRepository usos;
    @MockBean private CuponEventPublisher eventos;

    @Test
    void lasMigracionesCreanLasTablasYLasRestriccionesUnicas() {
        Cupon cupon = cupones.saveAndFlush(new Cupon("MIGRA", TipoCupon.PORCENTAJE, new BigDecimal("10"), null, null, 5));
        assertEquals(1, cupones.count());

        assertThrows(DataIntegrityViolationException.class, () ->
                cupones.saveAndFlush(new Cupon("MIGRA", TipoCupon.MONTO_FIJO, new BigDecimal("500"), null, null, null)),
                "el codigo es unico");

        usos.saveAndFlush(new CuponUso(cupon.getId(), "u1", new BigDecimal("1000"), new BigDecimal("100")));
        assertThrows(DataIntegrityViolationException.class, () ->
                usos.saveAndFlush(new CuponUso(cupon.getId(), "u1", new BigDecimal("2000"), new BigDecimal("200"))),
                "un usuario no puede usar el mismo cupon dos veces");
    }
}

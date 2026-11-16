package com.petshop.pago;

import com.petshop.pago.domain.AlertaFraude;
import com.petshop.pago.domain.EstadoPago;
import com.petshop.pago.domain.Pago;
import com.petshop.pago.messaging.PagoEventPublisher;
import com.petshop.pago.repository.AlertaFraudeRepository;
import com.petshop.pago.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ejecuta las migraciones Flyway REALES y deja que Hibernate valide que las entidades coinciden con las tablas.
 * Si una migracion queda incompleta, este test falla antes de llegar a la base de produccion.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:pago_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class MigracionesTest {

    @Autowired private PagoRepository pagos;
    @Autowired private AlertaFraudeRepository alertas;
    @MockBean private PagoEventPublisher publicador;

    @Test
    void lasMigracionesCreanLasTablasYElEsquemaCoincideConLasEntidades() {
        pagos.save(new Pago(1L, "usuario-1", new BigDecimal("1500.50"), "1111", "h".repeat(64), EstadoPago.APROBADO, null));
        alertas.save(new AlertaFraude("usuario-1", 1L, "1111", "h".repeat(64), 4, 10));

        assertEquals(1, pagos.findByUsuarioIdOrderByCreadoEnDesc("usuario-1").size());
        assertEquals(1, alertas.findTop50ByOrderByCreadoEnDesc().size());
    }
}

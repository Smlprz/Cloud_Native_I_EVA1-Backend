package com.petshop.orden.service;

import com.petshop.orden.domain.Orden;
import com.petshop.orden.repository.OrdenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Cola pago.procesado: el resultado del pago cambia el estado de la orden. */
@SpringBootTest
@ActiveProfiles("test")
class OrdenPagoTest {

    @Autowired
    private OrdenService service;

    @Autowired
    private OrdenRepository repository;

    private Long nuevaOrden() {
        return repository.save(new Orden("u1", new BigDecimal("1000.00"))).getId();
    }

    private String estado(Long id) {
        return repository.findById(id).orElseThrow().getEstado();
    }

    @Test
    void unPagoAprobadoDejaLaOrdenPagada() {
        Long id = nuevaOrden();
        assertThat(service.registrarPago(id, "APROBADO")).isTrue();
        assertThat(estado(id)).isEqualTo("PAGADA");
    }

    @Test
    void unPagoRechazadoDejaLaOrdenConPagoRechazado() {
        Long id = nuevaOrden();
        service.registrarPago(id, "RECHAZADO");
        assertThat(estado(id)).isEqualTo("PAGO_RECHAZADO");
    }

    @Test
    void unRechazoPosteriorNoDeshaceUnaOrdenPagada() {
        Long id = nuevaOrden();
        service.registrarPago(id, "APROBADO");
        service.registrarPago(id, "BLOQUEADO_FRAUDE");
        assertThat(estado(id)).isEqualTo("PAGADA");
    }

    @Test
    void unaOrdenInexistenteDevuelveFalse() {
        assertThat(service.registrarPago(999_999L, "APROBADO")).isFalse();
    }
}

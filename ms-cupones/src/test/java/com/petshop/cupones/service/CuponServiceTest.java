package com.petshop.cupones.service;

import com.petshop.cupones.domain.TipoCupon;
import com.petshop.cupones.repository.CuponRepository;
import com.petshop.cupones.repository.CuponUsoRepository;
import com.petshop.cupones.web.dto.CalculoResponse;
import com.petshop.cupones.web.dto.CuponRequest;
import com.petshop.cupones.web.dto.CuponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CuponServiceTest {

    @Autowired private CuponService service;
    @Autowired private CuponRepository cupones;
    @Autowired private CuponUsoRepository usos;

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    private static CuponRequest porcentaje(String codigo, String valor) {
        return new CuponRequest(codigo, TipoCupon.PORCENTAJE, pesos(valor), null, null, null);
    }

    private static HttpStatus estadoDe(Runnable accion) {
        return assertThrows(CuponException.class, accion::run).getStatus();
    }

    @BeforeEach
    void limpiar() {
        usos.deleteAll();
        cupones.deleteAll();
    }

    @Test
    void creaUnCuponYGuardaElCodigoEnMayusculas() {
        CuponResponse c = service.crear(porcentaje("verano10", "10"));

        assertEquals("VERANO10", c.codigo());
        assertEquals(0, c.usosActuales());
        assertTrue(c.activo());
        assertEquals(1, service.listar().size());
    }

    @Test
    void noPermiteCodigosRepetidosNiPorcentajesSobre100NiFechasPasadas() {
        service.crear(porcentaje("DUP", "10"));

        assertEquals(HttpStatus.CONFLICT, estadoDe(() -> service.crear(porcentaje("dup", "20"))));
        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(() -> service.crear(porcentaje("MUCHO", "101"))));
        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(() -> service.crear(
                new CuponRequest("PASADO", TipoCupon.MONTO_FIJO, pesos("1000"), null, LocalDateTime.now().minusDays(1), null))));
    }

    @Test
    void calculaElDescuentoPorPorcentajeYPorMontoFijo() {
        service.crear(porcentaje("DIEZ", "10"));
        service.crear(new CuponRequest("MIL", TipoCupon.MONTO_FIJO, pesos("1000"), null, null, null));

        CalculoResponse a = service.validar("u1", "diez", pesos("65980"));
        assertEquals(0, pesos("6598.00").compareTo(a.descuento()));
        assertEquals(0, pesos("59382.00").compareTo(a.total()));

        CalculoResponse b = service.validar("u1", "MIL", pesos("65980"));
        assertEquals(0, pesos("1000").compareTo(b.descuento()));
        assertEquals(0, pesos("64980").compareTo(b.total()));
    }

    @Test
    void elDescuentoNuncaDejaElTotalNegativo() {
        service.crear(new CuponRequest("GRANDE", TipoCupon.MONTO_FIJO, pesos("5000"), null, null, null));

        CalculoResponse r = service.validar("u1", "GRANDE", pesos("3000"));

        assertEquals(0, pesos("3000").compareTo(r.descuento()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.total()));
    }

    @Test
    void validarNoConsumeElCupon() {
        service.crear(new CuponRequest("UNO", TipoCupon.PORCENTAJE, pesos("10"), null, null, 1));

        service.validar("u1", "UNO", pesos("1000"));
        service.validar("u1", "UNO", pesos("1000"));

        assertEquals(0, service.listar().get(0).usosActuales());
        assertEquals(0, usos.count());
    }

    @Test
    void aplicarConsumeUnUsoYLoRegistra() {
        service.crear(new CuponRequest("UNO", TipoCupon.PORCENTAJE, pesos("10"), null, null, 5));

        service.aplicar("u1", "UNO", pesos("1000"));

        assertEquals(1, service.listar().get(0).usosActuales());
        assertEquals(1, usos.count());
    }

    @Test
    void unUsuarioSoloPuedeUsarUnCuponUnaVez() {
        service.crear(porcentaje("UNAVEZ", "10"));
        service.aplicar("u1", "UNAVEZ", pesos("1000"));

        assertEquals(HttpStatus.CONFLICT, estadoDe(() -> service.aplicar("u1", "UNAVEZ", pesos("1000"))));
        assertEquals(HttpStatus.CONFLICT, estadoDe(() -> service.validar("u1", "UNAVEZ", pesos("1000"))));
        service.aplicar("u2", "UNAVEZ", pesos("1000")); // otro usuario si puede
        assertEquals(2, service.listar().get(0).usosActuales());
    }

    @Test
    void respetaElMontoMinimoYLosCuponesInexistentesODesactivados() {
        service.crear(new CuponRequest("MINIMO", TipoCupon.PORCENTAJE, pesos("10"), pesos("50000"), null, null));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(() -> service.validar("u1", "MINIMO", pesos("49999"))));
        service.validar("u1", "MINIMO", pesos("50000")); // justo en el minimo: se puede
        assertEquals(HttpStatus.NOT_FOUND, estadoDe(() -> service.validar("u1", "NOEXISTE", pesos("1000"))));

        Long id = service.listar().get(0).id();
        service.cambiarActivo(id, false);
        assertEquals(HttpStatus.NOT_FOUND, estadoDe(() -> service.validar("u1", "MINIMO", pesos("60000"))));
        service.cambiarActivo(id, true);
        service.validar("u1", "MINIMO", pesos("60000"));
    }

    @Test
    void unCuponVencidoNoSePuedeUsar() {
        service.crear(new CuponRequest("TEMPORAL", TipoCupon.PORCENTAJE, pesos("10"), null,
                LocalDateTime.now().plusDays(1), null));
        Clock enDosDias = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(2));
        CuponService conReloj = new CuponService(cupones, usos, enDosDias);

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(() -> conReloj.validar("u1", "TEMPORAL", pesos("1000"))));
        service.validar("u1", "TEMPORAL", pesos("1000")); // hoy todavia vale
    }

    @Test
    void conUsosMaximosAgotadosNadieMasLoUsa() {
        service.crear(new CuponRequest("DOS", TipoCupon.PORCENTAJE, pesos("10"), null, null, 2));
        service.aplicar("u1", "DOS", pesos("1000"));
        service.aplicar("u2", "DOS", pesos("1000"));

        assertEquals(HttpStatus.CONFLICT, estadoDe(() -> service.aplicar("u3", "DOS", pesos("1000"))));
        assertEquals(HttpStatus.CONFLICT, estadoDe(() -> service.validar("u3", "DOS", pesos("1000"))));
        assertEquals(2, service.listar().get(0).usosActuales());
    }

    @Test
    void conMuchosUsuariosALaVezNuncaSePasaDeLosUsosMaximos() throws Exception {
        service.crear(new CuponRequest("CARRERA", TipoCupon.PORCENTAJE, pesos("10"), null, null, 3));

        int usuarios = 12;
        ExecutorService pool = Executors.newFixedThreadPool(usuarios);
        List<Callable<Boolean>> tareas = new ArrayList<>();
        for (int i = 0; i < usuarios; i++) {
            String usuario = "carrera-" + i;
            tareas.add(() -> {
                try {
                    service.aplicar(usuario, "CARRERA", pesos("1000"));
                    return true;
                } catch (CuponException e) {
                    return false;
                }
            });
        }
        int exitosos = 0;
        for (Future<Boolean> f : pool.invokeAll(tareas)) {
            if (f.get()) {
                exitosos++;
            }
        }
        pool.shutdown();

        assertEquals(3, exitosos, "exactamente 3 usuarios consiguen el cupon");
        assertEquals(3, service.listar().get(0).usosActuales());
        assertEquals(3, usos.count());
    }

    @Test
    void elMismoUsuarioConDosSolicitudesSimultaneasSoloLoUsaUnaVez() throws Exception {
        service.crear(porcentaje("DOBLECLIC", "10"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> intento = () -> {
            try {
                service.aplicar("impaciente", "DOBLECLIC", pesos("1000"));
                return true;
            } catch (CuponException e) {
                return false;
            }
        };
        List<Future<Boolean>> resultados = pool.invokeAll(List.of(intento, intento));
        pool.shutdown();

        long exitosos = resultados.stream().filter(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        }).count();
        assertEquals(1, exitosos);
        assertEquals(1, usos.count());
        assertEquals(1, service.listar().get(0).usosActuales(), "el contador tambien queda en 1 (se revirtio el segundo intento)");
        assertFalse(usos.findAll().isEmpty());
    }
}

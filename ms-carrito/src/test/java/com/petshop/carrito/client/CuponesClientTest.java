package com.petshop.carrito.client;

import com.petshop.carrito.service.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** El cliente hacia ms-cupones: reenvia el JWT, devuelve el descuento y traduce los errores a mensajes claros. */
class CuponesClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final CuponesClient cliente = new CuponesClient(builder, "http://cupones");

    @Test
    void aplicaElCuponReenviandoElTokenYDevuelveElDescuento() {
        servidor.expect(requestTo("http://cupones/api/cupones/aplicar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-usuario"))
                .andExpect(jsonPath("$.codigo").value("VERANO10"))
                .andExpect(jsonPath("$.monto").value(65980))
                .andRespond(withSuccess("{\"codigo\":\"VERANO10\",\"montoOriginal\":65980,\"descuento\":6598.00,\"total\":59382.00}",
                        MediaType.APPLICATION_JSON));

        CuponesClient.CuponAplicado r = cliente.aplicar("token-usuario", "VERANO10", new BigDecimal("65980"));

        assertEquals("VERANO10", r.codigo());
        assertEquals(0, new BigDecimal("6598.00").compareTo(r.descuento()));
        servidor.verify();
    }

    @Test
    void unCuponRechazadoMuestraElMotivoQueDioElServicio() {
        servidor.expect(requestTo("http://cupones/api/cupones/aplicar"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":409,\"detail\":\"Ya usaste este cupon\"}"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cliente.aplicar("t", "VERANO10", new BigDecimal("1000")));

        assertEquals("Ya usaste este cupon", ex.getMessage());
    }

    @Test
    void unCuponInexistenteDa404ConSuMensaje() {
        servidor.expect(requestTo("http://cupones/api/cupones/aplicar"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":404,\"detail\":\"El cupon no existe o no esta activo\"}"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cliente.aplicar("t", "NOEXISTE", new BigDecimal("1000")));

        assertEquals("El cupon no existe o no esta activo", ex.getMessage());
    }

    @Test
    void sinCuerpoUtilUsaUnMensajeGenerico() {
        servidor.expect(requestTo("http://cupones/api/cupones/aplicar")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cliente.aplicar("t", "X-1", new BigDecimal("1000")));

        assertEquals("El cupon no se pudo aplicar", ex.getMessage());
    }

    @Test
    void siMsCuponesFallaElCheckoutSeRechazaSinInventarUnDescuento() {
        servidor.expect(requestTo("http://cupones/api/cupones/aplicar")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cliente.aplicar("t", "VERANO10", new BigDecimal("1000")));

        assertEquals("No se pudo validar el cupon en este momento. Intenta de nuevo o compra sin cupon", ex.getMessage());
    }

    @Test
    void sinUrlConfiguradaLosCuponesNoEstanDisponibles() {
        CuponesClient sinServicio = new CuponesClient(RestClient.builder(), "");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sinServicio.aplicar("t", "VERANO10", new BigDecimal("1000")));

        assertEquals("Los cupones no estan disponibles por ahora", ex.getMessage());
    }
}

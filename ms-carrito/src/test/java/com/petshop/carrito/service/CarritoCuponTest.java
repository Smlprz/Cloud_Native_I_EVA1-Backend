package com.petshop.carrito.service;

import com.petshop.carrito.client.CuponesClient;
import com.petshop.carrito.client.CuponesClient.CuponAplicado;
import com.petshop.carrito.client.ProductoDto;
import com.petshop.carrito.client.ProductosClient;
import com.petshop.carrito.repository.PedidoRepository;
import com.petshop.carrito.web.dto.PedidoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El cupon cambia de verdad el total del pedido; si no sirve, el checkout se rechaza y el carrito queda intacto. */
@SpringBootTest
@ActiveProfiles("test")
class CarritoCuponTest {

    @Autowired private CarritoService service;
    @Autowired private PedidoRepository pedidos;
    @MockBean private ProductosClient productosClient;
    @MockBean private CuponesClient cupones;

    private void carritoConProducto(String usuario) {
        when(productosClient.obtenerProducto(eq(40L), any()))
                .thenReturn(new ProductoDto(40L, "Alimento Perro", new BigDecimal("32990.00"), 100, "Alimento Perros"));
        service.agregarItem(usuario, 40L, 2, "token"); // subtotal 65.980
    }

    @Test
    void conUnCuponValidoElTotalDelPedidoBajaYSeGuardaElDescuento() {
        carritoConProducto("cup-1");
        when(cupones.aplicar(eq("token-usuario"), eq("VERANO10"), any()))
                .thenReturn(new CuponAplicado("VERANO10", new BigDecimal("6598.00")));

        PedidoResponse pedido = service.checkout("cup-1", " verano10 ".trim().toUpperCase(), "token-usuario");

        assertThat(pedido.total()).isEqualByComparingTo("59382.00");
        assertThat(pedido.descuento()).isEqualByComparingTo("6598.00");
        assertThat(pedido.cuponCodigo()).isEqualTo("VERANO10");
        verify(cupones).aplicar("token-usuario", "VERANO10", new BigDecimal("65980.00")); // el cupon se calcula sobre el subtotal
        var guardado = pedidos.findById(pedido.id()).orElseThrow();
        assertThat(guardado.getTotal()).isEqualByComparingTo("59382.00");
        assertThat(guardado.getCuponCodigo()).isEqualTo("VERANO10");
    }

    @Test
    void sinCuponElPedidoQuedaIgualQueAntes() {
        carritoConProducto("cup-2");

        PedidoResponse pedido = service.checkout("cup-2");

        assertThat(pedido.total()).isEqualByComparingTo("65980.00");
        assertThat(pedido.descuento()).isEqualByComparingTo("0");
        assertThat(pedido.cuponCodigo()).isNull();
        verify(cupones, never()).aplicar(any(), any(), any());
    }

    @Test
    void unCuponQueNoSirveRechazaElCheckoutYElCarritoSigueAbierto() {
        carritoConProducto("cup-3");
        when(cupones.aplicar(any(), eq("VENCIDO"), any())).thenThrow(new ReglaNegocioException("El cupon esta vencido"));

        assertThatThrownBy(() -> service.checkout("cup-3", "VENCIDO", "token"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El cupon esta vencido");

        assertThat(service.verCarrito("cup-3").items()).hasSize(1);      // el carrito no se consumio
        assertThat(service.listarPedidos("cup-3")).isEmpty();             // y no se creo ningun pedido

        // al reintentar sin cupon, la compra sale normal
        assertThat(service.checkout("cup-3").total()).isEqualByComparingTo("65980.00");
    }

    @Test
    void unDescuentoMayorAlTotalDejaElTotalEnCeroNuncaNegativo() {
        carritoConProducto("cup-4");
        when(cupones.aplicar(any(), eq("REGALO"), any())).thenReturn(new CuponAplicado("REGALO", new BigDecimal("999999.00")));

        PedidoResponse pedido = service.checkout("cup-4", "REGALO", "token");

        assertThat(pedido.total()).isEqualByComparingTo("0");
    }

    @Test
    void uncuponEnBlancoSeIgnoraComoSiNoSeHubieraEnviado() {
        carritoConProducto("cup-5");

        PedidoResponse pedido = service.checkout("cup-5", "  ", "token");

        assertThat(pedido.total()).isEqualByComparingTo("65980.00");
        verify(cupones, never()).aplicar(any(), any(), any());
    }
}

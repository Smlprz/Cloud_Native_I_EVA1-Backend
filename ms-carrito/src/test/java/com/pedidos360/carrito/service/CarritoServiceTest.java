package com.pedidos360.carrito.service;

import com.pedidos360.carrito.client.ProductoDto;
import com.pedidos360.carrito.client.ProductosClient;
import com.pedidos360.carrito.web.dto.CarritoResponse;
import com.pedidos360.carrito.web.dto.PedidoResponse;
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
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CarritoServiceTest {

    @Autowired
    private CarritoService service;

    @MockBean
    private ProductosClient productosClient;

    @Test
    void agregarItemYHacerCheckoutGeneraPedidoConTotalCorrecto() {
        when(productosClient.obtenerProducto(eq(10L), any()))
                .thenReturn(new ProductoDto(10L, "Rascador Gato 137cm", new BigDecimal("39990.00"), 5));

        service.agregarItem("user-1", 10L, 2, "token-falso");

        CarritoResponse carrito = service.verCarrito("user-1");
        assertThat(carrito.items()).hasSize(1);
        assertThat(carrito.total()).isEqualByComparingTo("79980.00");

        PedidoResponse pedido = service.checkout("user-1");
        assertThat(pedido.total()).isEqualByComparingTo("79980.00");
        assertThat(pedido.items()).hasSize(1);

        // El carrito abierto quedo consumido
        assertThat(service.verCarrito("user-1").items()).isEmpty();
    }

    @Test
    void rechazaAgregarMasUnidadesQueElStockDisponible() {
        when(productosClient.obtenerProducto(eq(20L), any()))
                .thenReturn(new ProductoDto(20L, "Casa Perro Talla L", new BigDecimal("45990.00"), 3));

        assertThatThrownBy(() -> service.agregarItem("user-2", 20L, 4, "token-falso"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Stock insuficiente");
    }
}

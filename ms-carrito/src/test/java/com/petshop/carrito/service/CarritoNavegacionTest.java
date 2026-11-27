package com.petshop.carrito.service;

import com.petshop.carrito.client.ProductoDto;
import com.petshop.carrito.client.ProductosClient;
import com.petshop.carrito.messaging.kafka.NavegacionPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cada interaccion del usuario con el carrito se publica como evento de navegacion. */
@SpringBootTest
@ActiveProfiles("test")
class CarritoNavegacionTest {

    @Autowired private CarritoService service;
    @MockBean private ProductosClient productosClient;
    @MockBean private NavegacionPublisher navegacion;

    private void productoDisponible() {
        when(productosClient.obtenerProducto(eq(30L), any()))
                .thenReturn(new ProductoDto(30L, "Alimento Perro", new BigDecimal("10000.00"), 50, "Alimento Perros"));
    }

    @Test
    void agregarUnProductoPublicaItemAgregadoConSuCategoria() {
        productoDisponible();

        service.agregarItem("user-nav-1", 30L, 2, "token");

        verify(navegacion).itemAgregado(eq("user-nav-1"), eq(30L), eq("Alimento Perro"), eq("Alimento Perros"),
                eq(new BigDecimal("10000.00")), eq(2));
    }

    @Test
    void quitarUnProductoPublicaItemEliminado() {
        productoDisponible();
        Long itemId = service.agregarItem("user-nav-2", 30L, 3, "token").items().get(0).id();

        service.eliminarItem("user-nav-2", itemId);

        verify(navegacion).itemEliminado(eq("user-nav-2"), eq(30L), eq("Alimento Perro"),
                eq(new BigDecimal("10000.00")), eq(3));
    }

    @Test
    void elCheckoutPublicaUnaCompraPorCadaProducto() {
        productoDisponible();
        service.agregarItem("user-nav-3", 30L, 2, "token");

        service.checkout("user-nav-3");

        verify(navegacion).compraRealizada(eq("user-nav-3"), eq(30L), eq("Alimento Perro"),
                eq(new BigDecimal("10000.00")), eq(2));
        // Y gestiona el envio del pedido (ENVIO_GESTIONADO): total 20.000, 2 unidades.
        verify(navegacion).envioGestionado(eq("user-nav-3"), any(), eq(new BigDecimal("20000.00")), eq(2));
    }

    @Test
    void unaReglaDeNegocioQueFallaNoPublicaNada() {
        when(productosClient.obtenerProducto(eq(31L), any()))
                .thenReturn(new ProductoDto(31L, "Casa", new BigDecimal("1.00"), 1));

        org.junit.jupiter.api.Assertions.assertThrows(ReglaNegocioException.class,
                () -> service.agregarItem("user-nav-4", 31L, 5, "token")); // pide mas que el stock

        verify(navegacion, never()).itemAgregado(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
        assertThat(service.verCarrito("user-nav-4").items()).isEmpty();
    }
}

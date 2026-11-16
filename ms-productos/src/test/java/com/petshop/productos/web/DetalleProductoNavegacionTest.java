package com.petshop.productos.web;

import com.petshop.productos.domain.Product;
import com.petshop.productos.messaging.kafka.NavegacionPublisher;
import com.petshop.productos.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ver el detalle de un producto publica un evento de navegacion; listar el catalogo o un 404 no. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DetalleProductoNavegacionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository productos;
    @MockBean private NavegacionPublisher navegacion;

    private Long crearProducto() {
        return productos.save(new Product("Rascador Gato", "Alto", new BigDecimal("39990.00"), 10,
                "Gatos", "rascador.webp", 1L)).getId();
    }

    @Test
    void unVisitanteSinSesionQuedaComoAnonimo() throws Exception {
        Long id = crearProducto();

        mvc.perform(get("/api/productos/" + id)).andExpect(status().isOk());

        ArgumentCaptor<Product> producto = ArgumentCaptor.forClass(Product.class);
        verify(navegacion).productoVisto(eq("anonimo"), producto.capture());
        assertEquals("Gatos", producto.getValue().getCategoria());
    }

    @Test
    void conSesionSePublicaElUsuarioDelToken() throws Exception {
        Long id = crearProducto();

        mvc.perform(get("/api/productos/" + id)
                        .with(jwt().jwt(j -> j.claim("oid", "usuario-oid-123").subject("sub-1"))))
                .andExpect(status().isOk());

        verify(navegacion).productoVisto(eq("usuario-oid-123"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listarElCatalogoOUnProductoInexistenteNoPublicaNada() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/productos/987654")).andExpect(status().isNotFound());

        verify(navegacion, never()).productoVisto(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}

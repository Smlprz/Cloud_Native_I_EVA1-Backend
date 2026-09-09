package com.pedidos360.carrito.service;

import com.pedidos360.carrito.client.ProductoDto;
import com.pedidos360.carrito.client.ProductosClient;
import com.pedidos360.carrito.domain.Carrito;
import com.pedidos360.carrito.domain.CarritoItem;
import com.pedidos360.carrito.domain.Pedido;
import com.pedidos360.carrito.domain.PedidoItem;
import com.pedidos360.carrito.repository.CarritoRepository;
import com.pedidos360.carrito.repository.PedidoRepository;
import com.pedidos360.carrito.web.dto.CarritoResponse;
import com.pedidos360.carrito.web.dto.PedidoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica del carrito. Cada metodo es el limite transaccional: se devuelven DTOs
 * ya materializados para no depender de sesion Hibernate en la capa web
 * (open-in-view esta deshabilitado).
 */
@Service
@Transactional
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductosClient productosClient;

    public CarritoService(CarritoRepository carritoRepository,
                          PedidoRepository pedidoRepository,
                          ProductosClient productosClient) {
        this.carritoRepository = carritoRepository;
        this.pedidoRepository = pedidoRepository;
        this.productosClient = productosClient;
    }

    @Transactional(readOnly = true)
    public CarritoResponse verCarrito(String usuarioId) {
        Carrito carrito = carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.ABIERTO)
                .orElseGet(() -> new Carrito(usuarioId));
        return CarritoResponse.from(carrito);
    }

    public CarritoResponse agregarItem(String usuarioId, Long productoId, int cantidad, String bearerToken) {
        ProductoDto producto = productosClient.obtenerProducto(productoId, bearerToken);

        Carrito carrito = obtenerOCrear(usuarioId);

        CarritoItem existente = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .findFirst()
                .orElse(null);

        int cantidadFinal = cantidad + (existente != null ? existente.getCantidad() : 0);
        if (cantidadFinal > producto.stock()) {
            throw new ReglaNegocioException("Stock insuficiente para " + producto.nombre()
                    + " (disponible: " + producto.stock() + ")");
        }

        if (existente != null) {
            existente.setCantidad(cantidadFinal);
        } else {
            carrito.agregarItem(new CarritoItem(productoId, producto.nombre(), producto.precio(), cantidad));
        }
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    public CarritoResponse actualizarItem(String usuarioId, Long itemId, int cantidad) {
        Carrito carrito = obtenerCarritoAbierto(usuarioId);
        buscarItem(carrito, itemId).setCantidad(cantidad);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    public CarritoResponse eliminarItem(String usuarioId, Long itemId) {
        Carrito carrito = obtenerCarritoAbierto(usuarioId);
        carrito.quitarItem(buscarItem(carrito, itemId));
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    public void vaciar(String usuarioId) {
        carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.ABIERTO).ifPresent(carrito -> {
            carrito.getItems().clear();
            carritoRepository.save(carrito);
        });
    }

    public PedidoResponse checkout(String usuarioId) {
        Carrito carrito = obtenerCarritoAbierto(usuarioId);
        if (carrito.getItems().isEmpty()) {
            throw new ReglaNegocioException("El carrito esta vacio");
        }

        Pedido pedido = new Pedido(usuarioId);
        carrito.getItems().forEach(item -> pedido.agregarItem(new PedidoItem(
                item.getProductoId(),
                item.getNombreProducto(),
                item.getPrecioUnitario(),
                item.getCantidad())));
        pedido.recalcularTotal();

        carrito.setEstado(Carrito.CONFIRMADO);
        carritoRepository.save(carrito);
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidos(String usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    private Carrito obtenerOCrear(String usuarioId) {
        return carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.ABIERTO)
                .orElseGet(() -> carritoRepository.save(new Carrito(usuarioId)));
    }

    private Carrito obtenerCarritoAbierto(String usuarioId) {
        return carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.ABIERTO)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un carrito abierto"));
    }

    private CarritoItem buscarItem(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item " + itemId + " no esta en el carrito"));
    }
}

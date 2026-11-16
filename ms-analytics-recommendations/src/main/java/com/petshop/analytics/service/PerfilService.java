package com.petshop.analytics.service;

import com.petshop.analytics.event.NavegacionEvent;
import com.petshop.analytics.store.AnalyticsStore;
import com.petshop.analytics.store.PerfilUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Construye el perfil de navegacion de cada usuario a partir de sus eventos. Pesos: ver un producto = 1,
 * agregarlo al carrito = 3, comprarlo = 5, quitarlo del carrito = -1 (sobre la categoria del producto).
 */
@Service
public class PerfilService {

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);

    static final double PESO_VISTO = 1;
    static final double PESO_AGREGADO = 3;
    static final double PESO_COMPRA = 5;
    static final double PESO_QUITADO = -1;

    /** Los visitantes sin sesion comparten este id: no se les arma perfil. */
    static final String ANONIMO = "anonimo";

    private final AnalyticsStore store;
    private final CatalogoClient catalogo;

    public PerfilService(AnalyticsStore store, CatalogoClient catalogo) {
        this.store = store;
        this.catalogo = catalogo;
    }

    /** Guarda el evento y actualiza el perfil. Devuelve false si no hubo cambios (duplicado o anonimo). */
    public boolean aplicar(NavegacionEvent evento) {
        if (ANONIMO.equals(evento.usuarioId())) {
            return false;
        }
        if (!store.guardarEvento(evento)) {
            log.debug("Evento duplicado {} ignorado", evento.eventId());
            return false;
        }
        PerfilUsuario perfil = store.obtenerPerfil(evento.usuarioId());
        String categoria = categoriaDe(evento);

        switch (evento.tipo()) {
            case NavegacionEvent.PRODUCTO_VISTO -> {
                perfil.sumarCategoria(categoria, PESO_VISTO);
                perfil.registrarVisto(evento.productoId());
            }
            case NavegacionEvent.ITEM_AGREGADO -> {
                perfil.sumarCategoria(categoria, PESO_AGREGADO);
                perfil.registrarVisto(evento.productoId());
            }
            case NavegacionEvent.ITEM_ELIMINADO -> perfil.sumarCategoria(categoria, PESO_QUITADO);
            case NavegacionEvent.COMPRA_REALIZADA -> {
                perfil.sumarCategoria(categoria, PESO_COMPRA);
                perfil.registrarCompra(evento.productoId());
            }
            default -> { // cupones y envios: se guardan en el historial pero no cambian los gustos
            }
        }
        perfil.marcarActualizado(evento.timestamp());
        store.guardarPerfil(perfil);
        return true;
    }

    public PerfilUsuario perfilDe(String usuarioId) {
        return store.obtenerPerfil(usuarioId);
    }

    /** La categoria viene en el evento si el publicador la conocia; si no, se busca en el catalogo. */
    private String categoriaDe(NavegacionEvent evento) {
        if (evento.categoria() != null && !evento.categoria().isBlank()) {
            return evento.categoria();
        }
        if (evento.productoId() == null) {
            return null;
        }
        return catalogo.buscar(evento.productoId()).map(ProductoCatalogo::categoria).orElse(null);
    }
}

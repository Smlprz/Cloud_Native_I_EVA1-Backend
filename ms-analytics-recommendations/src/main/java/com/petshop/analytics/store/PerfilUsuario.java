package com.petshop.analytics.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Perfil de navegacion de un usuario, construido solo con sus eventos:
 *  - categorias: puntaje por categoria (ver = 1, agregar al carrito = 3, comprar = 5, quitar = -1)
 *  - vistosRecientes: ultimos productos que miro (el mas reciente primero)
 *  - comprados: productos que ya compro (no se le vuelven a recomendar)
 */
public class PerfilUsuario {

    /** Cuantos productos vistos recientemente se recuerdan. */
    public static final int MAX_VISTOS = 10;

    private final String usuarioId;
    private final Map<String, Double> categorias = new HashMap<>();
    private final List<Long> vistosRecientes = new ArrayList<>();
    private final Set<Long> comprados = new LinkedHashSet<>();
    private Instant actualizado = Instant.now();

    public PerfilUsuario(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** Suma (o resta) puntos a una categoria; el puntaje nunca baja de cero. */
    public void sumarCategoria(String categoria, double puntos) {
        if (categoria == null || categoria.isBlank()) {
            return;
        }
        double nuevo = Math.max(0, categorias.getOrDefault(categoria, 0.0) + puntos);
        if (nuevo == 0) {
            categorias.remove(categoria);
        } else {
            categorias.put(categoria, nuevo);
        }
    }

    public void registrarVisto(Long productoId) {
        vistosRecientes.remove(productoId);
        vistosRecientes.add(0, productoId);
        while (vistosRecientes.size() > MAX_VISTOS) {
            vistosRecientes.remove(vistosRecientes.size() - 1);
        }
    }

    public void registrarCompra(Long productoId) {
        comprados.add(productoId);
    }

    public void marcarActualizado(Instant momento) {
        this.actualizado = momento;
    }

    public String getUsuarioId() { return usuarioId; }
    public Map<String, Double> getCategorias() { return categorias; }
    public List<Long> getVistosRecientes() { return vistosRecientes; }
    public Set<Long> getComprados() { return comprados; }
    public Instant getActualizado() { return actualizado; }
}

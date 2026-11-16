package com.petshop.analytics.service;

import com.petshop.analytics.store.AnalyticsStore;
import com.petshop.analytics.store.PerfilUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Recomienda productos parecidos a los que el usuario ha consultado recientemente.
 *
 * Puntaje de un producto candidato:
 *   afinidad de su categoria en el perfil del usuario   (ver = 1, carrito = 3, compra = 5 por interaccion)
 *   + 2 si es de la misma categoria que el producto que esta mirando ahora
 *   + hasta 0.5 por popularidad (visitas de todos los usuarios, normalizadas)
 *   - 0.5 si ya lo vio hace poco
 * Se descartan los productos sin stock, el que esta mirando y los que ya compro.
 * Sin historial (usuario nuevo) se recomiendan los mas vistos.
 */
@Service
public class RecomendacionService {

    static final double BONUS_PRODUCTO_ACTUAL = 2.0;
    static final double PESO_POPULARIDAD = 0.5;
    static final double PENALIZA_YA_VISTO = 0.5;

    private final AnalyticsStore store;
    private final CatalogoClient catalogo;
    private final MetricasService metricas;
    private final int limitePorDefecto;

    public RecomendacionService(AnalyticsStore store, CatalogoClient catalogo, MetricasService metricas,
                                @Value("${analytics.recomendaciones.limite:4}") int limitePorDefecto) {
        this.store = store;
        this.catalogo = catalogo;
        this.metricas = metricas;
        this.limitePorDefecto = limitePorDefecto;
    }

    public List<Recomendacion> recomendar(String usuarioId, Long productoActualId, Integer limite) {
        int cuantos = limite != null ? Math.min(Math.max(limite, 1), 20) : limitePorDefecto;
        PerfilUsuario perfil = store.obtenerPerfil(usuarioId);
        List<ProductoCatalogo> productos = catalogo.catalogo();

        Map<String, Double> afinidad = new HashMap<>(perfil.getCategorias());
        Optional<ProductoCatalogo> actual = productoActualId == null ? Optional.empty() : catalogo.buscar(productoActualId);
        actual.ifPresent(p -> afinidad.merge(p.categoria(), BONUS_PRODUCTO_ACTUAL, Double::sum));

        Set<Long> excluidos = new HashSet<>(perfil.getComprados());
        if (productoActualId != null) {
            excluidos.add(productoActualId);
        }
        long masVistas = productos.stream().mapToLong(p -> metricas.vistas(p.id())).max().orElse(0);
        boolean sinHistorial = afinidad.isEmpty();

        return productos.stream()
                .filter(p -> p.stock() > 0 && !excluidos.contains(p.id()))
                .map(p -> puntuar(p, afinidad, perfil, masVistas, sinHistorial, actual))
                .filter(r -> sinHistorial || r.puntaje() > 0)
                .sorted(Comparator.comparingDouble(Recomendacion::puntaje).reversed()
                        .thenComparing(Recomendacion::productoId))
                .limit(cuantos)
                .toList();
    }

    private Recomendacion puntuar(ProductoCatalogo p, Map<String, Double> afinidad, PerfilUsuario perfil,
                                  long masVistas, boolean sinHistorial, Optional<ProductoCatalogo> actual) {
        double popularidad = masVistas == 0 ? 0 : PESO_POPULARIDAD * metricas.vistas(p.id()) / masVistas;
        double puntaje = afinidad.getOrDefault(p.categoria(), 0.0) + popularidad;
        if (perfil.getVistosRecientes().contains(p.id())) {
            puntaje -= PENALIZA_YA_VISTO;
        }

        String motivo;
        if (sinHistorial) {
            motivo = "Uno de los productos mas vistos";
        } else if (actual.isPresent() && p.categoria() != null && p.categoria().equals(actual.get().categoria())) {
            motivo = "Parecido a " + actual.get().nombre();
        } else {
            motivo = "Porque te interesa " + p.categoria();
        }
        return new Recomendacion(p.id(), p.nombre(), p.descripcion(), p.precio(), p.categoria(), p.imagenUrl(),
                motivo, Math.round(puntaje * 100) / 100.0);
    }
}

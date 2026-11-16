package com.petshop.analytics.web;

import com.petshop.analytics.service.MetricasService;
import com.petshop.analytics.service.MetricasSnapshot;
import com.petshop.analytics.service.PerfilService;
import com.petshop.analytics.service.Recomendacion;
import com.petshop.analytics.service.RecomendacionService;
import com.petshop.analytics.store.PerfilUsuario;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API de analitica. Exige JWT de Azure AD con scope productos.read o carrito.read y rol CLIENTE o ADMIN.
 * Las metricas son solo para ADMIN; en tiempo real llegan ademas por WebSocket (/ws/metricas).
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final String SCOPES = "hasAnyAuthority('SCOPE_productos.read','SCOPE_carrito.read','SCOPE_ordenes.read')";

    private final RecomendacionService recomendaciones;
    private final PerfilService perfiles;
    private final MetricasService metricas;

    public AnalyticsController(RecomendacionService recomendaciones, PerfilService perfiles, MetricasService metricas) {
        this.recomendaciones = recomendaciones;
        this.perfiles = perfiles;
        this.metricas = metricas;
    }

    /** Productos parecidos a los que el usuario consulto; productoId = el que esta mirando ahora (opcional). */
    @GetMapping("/recomendaciones")
    @PreAuthorize(SCOPES + " and hasAnyRole('CLIENTE','ADMIN')")
    public List<Recomendacion> recomendaciones(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) @Positive Long productoId,
                                               @RequestParam(required = false) @Min(1) @Max(20) Integer limite) {
        return recomendaciones.recomendar(UsuarioActual.id(jwt), productoId, limite);
    }

    /** El perfil de navegacion del propio usuario: que categorias le interesan y que miro. */
    @GetMapping("/perfil")
    @PreAuthorize(SCOPES + " and hasAnyRole('CLIENTE','ADMIN')")
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        return PerfilResponse.from(perfiles.perfilDe(UsuarioActual.id(jwt)));
    }

    /** Foto actual de las metricas (la misma que se difunde por WebSocket). */
    @GetMapping("/metricas")
    @PreAuthorize(SCOPES + " and hasRole('ADMIN')")
    public MetricasSnapshot metricas() {
        return metricas.snapshot();
    }

    public record PerfilResponse(String usuarioId, Map<String, Double> categorias, List<Long> vistosRecientes,
                                 List<Long> comprados, Instant actualizado) {

        static PerfilResponse from(PerfilUsuario p) {
            return new PerfilResponse(p.getUsuarioId(), p.getCategorias(), List.copyOf(p.getVistosRecientes()),
                    List.copyOf(p.getComprados()), p.getActualizado());
        }
    }
}

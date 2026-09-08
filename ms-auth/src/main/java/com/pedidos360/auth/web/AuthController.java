package com.pedidos360.auth.web;

import com.pedidos360.auth.domain.Acceso;
import com.pedidos360.auth.repository.AccesoRepository;
import com.pedidos360.auth.web.dto.AccesoResponse;
import com.pedidos360.auth.web.dto.PerfilResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Microservicio de identidad (BFF). El login lo hace Azure AD;
 * aqui se expone el perfil del usuario autenticado y se auditan los accesos.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccesoRepository accesos;

    public AuthController(AccesoRepository accesos) {
        this.accesos = accesos;
    }

    /** Perfil del usuario a partir del JWT. Registra el acceso en la BD de auditoria. */
    @GetMapping("/me")
    @Transactional
    public PerfilResponse me(@AuthenticationPrincipal Jwt jwt,
                             Authentication authentication,
                             HttpServletRequest request) {
        List<String> roles = UsuarioActual.roles(authentication);
        List<String> scopes = UsuarioActual.scopes(authentication);

        accesos.save(new Acceso(
                UsuarioActual.id(jwt),
                UsuarioActual.nombre(jwt),
                UsuarioActual.email(jwt),
                String.join(",", roles),
                UsuarioActual.ip(request)));

        return new PerfilResponse(
                UsuarioActual.id(jwt),
                UsuarioActual.nombre(jwt),
                UsuarioActual.email(jwt),
                roles,
                scopes);
    }

    /**
     * Historial de accesos. Un usuario ve los suyos; un ADMIN ve todos.
     */
    @GetMapping("/accesos")
    @Transactional(readOnly = true)
    public List<AccesoResponse> accesos(@AuthenticationPrincipal Jwt jwt,
                                        Authentication authentication,
                                        @RequestParam(defaultValue = "50") int limite) {
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limite, 1), 200));
        boolean esAdmin = UsuarioActual.roles(authentication).contains("ADMIN");

        Page<Acceso> resultado = esAdmin
                ? accesos.findAll(page)
                : accesos.findByUsuarioIdOrderByCreadoEnDesc(UsuarioActual.id(jwt), page);

        return resultado.getContent().stream().map(AccesoResponse::from).toList();
    }

    /** Endpoint simple y publico para comprobaciones (ademas de /actuator/health). */
    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP", "service", "ms-auth");
    }
}

package com.petshop.analytics.ws;

import com.petshop.analytics.security.AzureJwtConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Valida el JWT de Azure ANTES de abrir el WebSocket. Un navegador no puede enviar el encabezado Authorization en
 * una conexion WebSocket, asi que el token viaja en la URL (wss://host/ws/metricas?token=...). Se verifica con el
 * mismo decodificador que la API REST (firma, vigencia, emisor y audiencia) y se exige rol ADMIN.
 * Sin token valido la conexion se rechaza con 401 y sin rol ADMIN con 403.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtDecoder jwtDecoder;
    private final List<String> admins;
    private final List<String> vendedores;

    public JwtHandshakeInterceptor(JwtDecoder jwtDecoder,
                                   @Value("${app.roles.admin:}") List<String> admins,
                                   @Value("${app.roles.vendedor:}") List<String> vendedores) {
        this.jwtDecoder = jwtDecoder;
        this.admins = admins;
        this.vendedores = vendedores;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        Rechazo rechazo = validar(tokenDe(request));
        return rechazo == null || rechazar(response, rechazo.status(), rechazo.motivo());
    }

    /** null si el token es valido y de un ADMIN; si no, el codigo y el motivo del rechazo. */
    public Rechazo validar(String token) {
        if (token == null || token.isBlank()) {
            return new Rechazo(HttpStatus.UNAUTHORIZED, "falta el token");
        }
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException e) {
            return new Rechazo(HttpStatus.UNAUTHORIZED, "token invalido: " + e.getMessage());
        }
        boolean esAdmin = new AzureJwtConverter(admins, vendedores).convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return esAdmin ? null : new Rechazo(HttpStatus.FORBIDDEN, "se requiere rol ADMIN");
    }

    public record Rechazo(HttpStatus status, String motivo) {
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // nada que hacer
    }

    private static String tokenDe(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servlet) {
            return servlet.getServletRequest().getParameter("token");
        }
        return null;
    }

    private static boolean rechazar(ServerHttpResponse response, HttpStatus estado, String motivo) {
        log.warn("WebSocket rechazado ({}): {}", estado.value(), motivo);
        response.setStatusCode(estado);
        return false;
    }
}

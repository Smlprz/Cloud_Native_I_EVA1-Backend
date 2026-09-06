package com.pedidos360.carrito.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae el identificador estable del usuario desde el token de Azure AD. */
final class UsuarioActual {

    private UsuarioActual() {
    }

    static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }
}

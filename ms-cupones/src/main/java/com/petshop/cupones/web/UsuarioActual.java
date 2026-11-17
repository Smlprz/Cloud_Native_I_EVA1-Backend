package com.petshop.cupones.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae la identidad del usuario desde el token de Azure AD (mismo criterio que el resto de los servicios). */
final class UsuarioActual {

    private UsuarioActual() {
    }

    static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }
}

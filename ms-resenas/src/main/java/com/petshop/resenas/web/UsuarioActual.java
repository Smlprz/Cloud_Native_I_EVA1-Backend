package com.petshop.resenas.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae la identidad del usuario desde el token de Azure AD. */
final class UsuarioActual {

    private UsuarioActual() {
    }

    static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    static String nombre(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("preferred_username");
    }
}

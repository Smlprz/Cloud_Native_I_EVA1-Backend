package com.petshop.orden.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae la identidad del usuario desde el token de Azure AD. */
final class UsuarioActual {

    private UsuarioActual() {
    }

    static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    static String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email != null && email.contains("@") ? email : null;
    }
}

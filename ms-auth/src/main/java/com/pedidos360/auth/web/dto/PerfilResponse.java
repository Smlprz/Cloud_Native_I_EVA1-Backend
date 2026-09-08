package com.pedidos360.auth.web.dto;

import java.util.List;

/** Perfil del usuario autenticado, derivado del JWT de Azure AD. */
public record PerfilResponse(
        String id,
        String nombre,
        String email,
        List<String> roles,
        List<String> scopes
) {
}

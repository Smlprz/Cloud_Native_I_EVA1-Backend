package com.pedidos360.productos.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Verifica que el claim "aud" del token corresponda a esta API.
 * Azure v2 puede emitir la audiencia como "api://<client-id>" o solo "<client-id>":
 * se aceptan ambas formas.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> aceptadas;

    public AudienceValidator(String expectedAudience) {
        String sinPrefijo = expectedAudience.startsWith("api://")
                ? expectedAudience.substring("api://".length())
                : expectedAudience;
        this.aceptadas = List.of(expectedAudience, sinPrefijo, "api://" + sinPrefijo);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> aud = token.getAudience();
        if (aud != null && aud.stream().anyMatch(aceptadas::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "El token no contiene la audiencia requerida (" + aceptadas + "), aud=" + aud,
                null));
    }
}

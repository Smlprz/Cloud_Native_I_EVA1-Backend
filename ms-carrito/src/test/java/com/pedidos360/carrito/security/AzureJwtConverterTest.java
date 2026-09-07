package com.pedidos360.carrito.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la resolucion de roles en el backend cuando el tenant de Azure
 * no puede insertar el claim "roles".
 */
class AzureJwtConverterTest {

    private final AzureJwtConverter converter =
            new AzureJwtConverter(List.of("jefe@instituto.cl"), List.of("vendedor@instituto.cl"));

    private static Jwt jwt(java.util.Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        claims.forEach(b::claim);
        return b.build();
    }

    private List<String> authorities(Jwt jwt) {
        return AuthorityUtils.authorityListToSet(converter.convert(jwt).getAuthorities())
                .stream().sorted().toList();
    }

    @Test
    void sinClaimRolesYEmailDesconocidoAsignaCliente() {
        var auth = authorities(jwt(java.util.Map.of(
                "scp", "carrito.read carrito.write",
                "preferred_username", "random@instituto.cl")));
        assertThat(auth).contains("ROLE_CLIENTE", "SCOPE_carrito.read", "SCOPE_carrito.write");
        assertThat(auth).doesNotContain("ROLE_ADMIN", "ROLE_VENDEDOR");
    }

    @Test
    void emailEnListaDeAdminAsignaAdmin() {
        var auth = authorities(jwt(java.util.Map.of("preferred_username", "JEFE@instituto.cl")));
        assertThat(auth).contains("ROLE_ADMIN");
    }

    @Test
    void emailEnListaDeVendedorAsignaVendedor() {
        var auth = authorities(jwt(java.util.Map.of("email", "vendedor@instituto.cl")));
        assertThat(auth).contains("ROLE_VENDEDOR");
    }

    @Test
    void siElTokenTraeClaimRolesSeUsaEseYNoElFallback() {
        var auth = authorities(jwt(java.util.Map.of(
                "roles", List.of("ADMIN"),
                "preferred_username", "random@instituto.cl")));
        assertThat(auth).contains("ROLE_ADMIN");
        assertThat(auth).doesNotContain("ROLE_CLIENTE");
    }

    @Test
    void normalizaLosValoresDeAppRoleDeAzure() {
        var auth = authorities(jwt(java.util.Map.of(
                "roles", List.of("Administrador", "Vendedor", "Cliente"))));
        assertThat(auth).contains("ROLE_ADMIN", "ROLE_VENDEDOR", "ROLE_CLIENTE");
    }

    @Test
    void canonicalizarRolToleraVariantes() {
        assertThat(AzureJwtConverter.canonicalizarRol("administrador")).isEqualTo("ADMIN");
        assertThat(AzureJwtConverter.canonicalizarRol("ADMIN")).isEqualTo("ADMIN");
        assertThat(AzureJwtConverter.canonicalizarRol("Vendedor")).isEqualTo("VENDEDOR");
        assertThat(AzureJwtConverter.canonicalizarRol("Cliente")).isEqualTo("CLIENTE");
    }
}

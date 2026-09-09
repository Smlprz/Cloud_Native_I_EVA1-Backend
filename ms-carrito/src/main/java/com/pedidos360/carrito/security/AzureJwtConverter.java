package com.pedidos360.carrito.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduce los claims de un token de Azure AD a autoridades de Spring Security.
 *
 *  - claim "scp"  -> autoridades  SCOPE_<scope>   (permisos delegados de la API).
 *  - roles:
 *      1) si el token trae el claim "roles" (App Roles de Azure), se normaliza cada
 *         valor a ADMIN / VENDEDOR / CLIENTE (tolera "Administrador", "admin",
 *         "Vendedor", "Cliente", con o sin acentos y en cualquier caja).
 *      2) si NO lo trae, el rol se resuelve por el email del usuario contra las
 *         listas app.roles.admin / app.roles.vendedor; el resto queda como CLIENTE.
 */
public class AzureJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    private final Set<String> admins;
    private final Set<String> vendedores;

    public AzureJwtConverter(Collection<String> admins, Collection<String> vendedores) {
        this.admins = normalizarEmails(admins);
        this.vendedores = normalizarEmails(vendedores);

        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthorityPrefix("SCOPE_");
        scopes.setAuthoritiesClaimName("scp");

        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            authorities.addAll(resolverRoles(jwt));
            return authorities;
        });
    }

    private Collection<GrantedAuthority> resolverRoles(Jwt jwt) {
        List<String> rolesClaim = jwt.getClaimAsStringList("roles");
        if (rolesClaim != null && !rolesClaim.isEmpty()) {
            return rolesClaim.stream()
                    .map(AzureJwtConverter::canonicalizarRol)
                    .distinct()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        }

        String email = primerNoNulo(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("upn"));

        String rol = "CLIENTE";
        if (email != null) {
            String e = email.toLowerCase(Locale.ROOT);
            if (admins.contains(e)) {
                rol = "ADMIN";
            } else if (vendedores.contains(e)) {
                rol = "VENDEDOR";
            }
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    /** "Administrador" / "admin" -> ADMIN ; "Vendedor" -> VENDEDOR ; "Cliente" -> CLIENTE. */
    static String canonicalizarRol(String valor) {
        String v = sinAcentos(valor).toUpperCase(Locale.ROOT).trim();
        if (v.startsWith("ADMIN")) {
            return "ADMIN";
        }
        if (v.startsWith("VEND") || v.startsWith("SELL")) {
            return "VENDEDOR";
        }
        if (v.startsWith("CLIENT") || v.startsWith("CUSTOM")) {
            return "CLIENTE";
        }
        return v;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        return delegate.convert(source);
    }

    private static Set<String> normalizarEmails(Collection<String> valores) {
        Set<String> set = new HashSet<>();
        if (valores != null) {
            for (String v : valores) {
                if (v != null && !v.isBlank()) {
                    set.add(v.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return set;
    }

    private static String sinAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String primerNoNulo(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}

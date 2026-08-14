package com.bioinformatics.common.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts authorities from the {@code roles} claim (comma-separated).
 * <p>Compatible with tokens issued by the monolith's {@code JwtUtil}.
 */
public class CustomJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtAuthenticationConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var rolesClaim = jwt.getClaimAsString("roles");
        if (rolesClaim == null || rolesClaim.isBlank()) {
            return Collections.emptyList();
        }
        return Stream.of(rolesClaim.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
package com.bioinformatics.common.config.jwt;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Convenience helper to extract information from the currently-authenticated
 * JWT without pulling in the monolith's {@code JwtUtil}.
 */
@Component
public class JwtContext {

    private Optional<JwtAuthenticationToken> currentJwtAuth() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth instanceof JwtAuthenticationToken jwtAuth && jwtAuth.isAuthenticated())
                ? Optional.of(jwtAuth)
                : Optional.empty();
    }

    public Optional<String> getCurrentUsername() {
        return currentJwtAuth().map(a -> a.getToken().getSubject());
    }

    public Optional<String> getCurrentUserId() {
        return currentJwtAuth().map(a -> a.getToken().getClaimAsString("userId"));
    }

    public List<String> getCurrentUserRoles() {
        return currentJwtAuth()
                .map(a -> a.getToken().getClaimAsString("roles"))
                .map(r -> Arrays.asList(r.split(",")))
                .orElse(Collections.emptyList());
    }

    public boolean hasRole(String role) {
        var roles = getCurrentUserRoles();
        var target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles.contains(target);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
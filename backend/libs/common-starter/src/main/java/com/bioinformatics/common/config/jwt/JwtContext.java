package com.bioinformatics.common.config.jwt;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bioinformatics.shared.models.security.AppClaims.ROLES;
import static com.bioinformatics.shared.models.security.AppClaims.USER_ID;
import static com.bioinformatics.shared.models.security.Constants.ADMIN_ROLE;
import static com.bioinformatics.shared.models.security.Constants.ROLE_PREFIX;

/**
 * Convenience access helper for extracting the authenticated JWT subject and claims from the
 * current Spring Security context.
 *
 * <p>This keeps controllers and services from reaching into security internals while still enabling
 * straightforward checks for the current user identity, roles, and admin status.
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
        return currentJwtAuth().map(a -> a.getToken().getClaimAsString(USER_ID.getClaim()));
    }

    public List<String> getCurrentUserRoles() {
        return currentJwtAuth()
                .map(a -> a.getToken().getClaimAsString(ROLES.getClaim()))
                .map(r -> Arrays.asList(r.split(",")))
                .orElse(Collections.emptyList());
    }

    public boolean hasRole(String role) {
        var roles = getCurrentUserRoles();
        var target = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return roles.contains(target);
    }

    public boolean isAdmin() {
        return hasRole(ADMIN_ROLE);
    }
}
package com.bioinformatics.common.config.web;

import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the currently authenticated user from the Spring Security context.
 *
 * <p>This is used by {@link CurrentUserArgumentResolver} to inject a {@link UserPrincipal}
 * into controller method parameters without coupling the controller to SecurityContext internals.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticatedUserResolver {

    /**
     * Retrieves the current authenticated user, if present.
     *
     * @return optional user principal extracted from the security context
     */
    public Optional<UserPrincipal> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.debug("[COMMON_AUTH] No authentication object in security context");
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal user)) {
            log.debug("[COMMON_AUTH] Authentication principal is not a UserPrincipal - type={}",
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
            return Optional.empty();
        }

        log.debug("[COMMON_AUTH] Current user resolved - userId='{}', roles={}", user.id(), user.roles());
        return Optional.of(user);
    }
}
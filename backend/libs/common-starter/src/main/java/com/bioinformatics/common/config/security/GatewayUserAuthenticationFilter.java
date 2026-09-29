package com.bioinformatics.common.config.security;

import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.bioinformatics.shared.models.security.AppHeaders.*;
import static com.bioinformatics.shared.models.security.Constants.ROLE_PREFIX;

/**
 * Gateway-supplied authentication filter.
 *
 * <p>This filter reconstructs the authenticated principal from headers forwarded by the API gateway,
 * allowing downstream services to work with standard Spring Security access checks without directly
 * reading gateway-specific metadata in each controller or service.
 */
@Component
@Slf4j
public class GatewayUserAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Creates the authentication principal from gateway headers.
     *
     * @param userId       unique user identifier from gateway
     * @param roles        role list forwarded by gateway
     * @param dataProvider provider hint used for downstream routing
     * @return authentication token with a {@link UserPrincipal}
     */
    private static UsernamePasswordAuthenticationToken getAuthentication(final String userId, final List<String> roles, final String dataProvider) {
        var principal = new UserPrincipal(userId, roles, dataProvider);

        var authorities = roles
                .stream()
                .map(role -> (role != null && !role.startsWith(ROLE_PREFIX))
                        ? ROLE_PREFIX + role
                        : (role != null ? role : ROLE_PREFIX.concat(USER_ROLE.getDefaultValue())))
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    /**
     * Resolves gateway headers and injects a Spring Security authentication context for the request.
     *
     * @param request current servlet request
     * @param response current servlet response
     * @param filterChain next filter in the chain
     * @throws ServletException if the request cannot be processed
     * @throws IOException if the downstream chain fails
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var method = request.getMethod();
        var servletPath = request.getServletPath();
        log.debug("[COMMON_AUTH] Processing authenticated request - method={}, path={}", method, servletPath);

        var userId = request.getHeader(USER_ID.getHeader());
        var roles = new ArrayList<String>();
        request.getHeaders(USER_ROLE.getHeader()).asIterator().forEachRemaining(roles::add);
        var dataProvider = request.getHeader(DATA_PROVIDER.getHeader());

        if (userId != null && !userId.isBlank()) {
            log.debug("[COMMON_AUTH] Gateway headers resolved - userId='{}', roles={}, dataProvider='{}'",
                    userId, roles, dataProvider);
            var authentication = getAuthentication(userId, roles, dataProvider);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[COMMON_AUTH] Security context populated for userId='{}'", userId);
        } else {
            log.debug("[COMMON_AUTH] No gateway user context found for path={} - proceeding without authenticated principal", servletPath);
        }

        filterChain.doFilter(request, response);
    }

}

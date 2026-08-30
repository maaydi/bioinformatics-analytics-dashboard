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

@Component
@Slf4j
public class GatewayUserAuthenticationFilter extends OncePerRequestFilter {



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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("GatewayUserAuthenticationFilter Request : {} {}", request.getMethod(), request.getServletPath());

        var userId = request.getHeader(USER_ID.getHeader());
        log.debug("UserId : {}", userId);
        var roles = new ArrayList<String>();
        request.getHeaders(USER_ROLE.getHeader()).asIterator().forEachRemaining(roles::add);
        log.debug("Roles : {}", roles);
        var dataProvider = request.getHeader(DATA_PROVIDER.getHeader());
        log.debug("DataProvider : {}", dataProvider);

        if (userId != null && !userId.isBlank()) {
            log.debug("Create UsernamePasswordAuthenticationToken For UserId : {}", userId);
            var authentication = getAuthentication(userId, roles, dataProvider);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        log.debug("Authentication : {}", SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);
    }

}

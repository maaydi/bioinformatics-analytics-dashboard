package com.bioinformatics.common.config.security;

import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.bioinformatics.shared.models.security.AppHeaders.*;

@Component
public class GatewayUserAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE = "ROLE_";

    private static UsernamePasswordAuthenticationToken getAuthentication(final String userId, final String role, final String dataProvider) {
        var principal = new UserPrincipal(userId, role, dataProvider);

        var formattedRole = (role != null && !role.startsWith(ROLE))
                ? ROLE + role
                : (role != null ? role : ROLE.concat(USER_ROLE.getDefaultValue()));

        var authorities = List.of(new SimpleGrantedAuthority(formattedRole));

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        var userId = request.getHeader(USER_ID.getHeader());
        var role = request.getHeader(USER_ROLE.getHeader());
        var dataProvider = request.getHeader(DATA_PROVIDER.getHeader());

        if (userId != null && !userId.isBlank()) {
            var authentication = getAuthentication(userId, role, dataProvider);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}

package com.bioinformatics.authservice.security;

import com.bioinformatics.authservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Populates the security context from access JWTs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && PUBLIC_ENDPOINTS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var jwt = authHeader.substring(7);
        final String username;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            log.debug("JWT parsing failed: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var userDetails = userDetailsService.loadUserByUsername(username);
                if (!jwtService.isRefreshToken(jwt) && jwtService.isTokenValid(jwt, userDetails)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ex) {
                log.debug("User from JWT not found: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}


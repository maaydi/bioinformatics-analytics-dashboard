package com.bioinformatics.common.providers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static com.bioinformatics.shared.models.security.AppHeaders.DATA_PROVIDER;

/**
 * HTTP filter that reads the X-Data-Provider header and sets the active provider for the request.
 * Default provider is "postgres" if header is absent or empty.
 * Filter runs at highest precedence to ensure context is set early.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProviderFilter extends OncePerRequestFilter {

    /**
     * Intercept request, set provider context, and clean up after response.
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain servlet filter chain
     * @throws ServletException on filter error
     * @throws IOException      on I/O error
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            var provider = request.getHeader(DATA_PROVIDER.getHeader());
            ProviderContextHolder.set(Objects.requireNonNullElse(provider, DATA_PROVIDER.getDefaultValue()));
            filterChain.doFilter(request, response);
        } finally {
            ProviderContextHolder.clear();
        }
    }
}

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
 * HTTP filter that reads the {@code X-Data-Provider} header and sets the active provider for the request.
 *
 * <p>If the header is absent or empty, the filter falls back to the default provider configured in the
 * shared header metadata. The context is always cleared in the finally block to prevent leaks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProviderFilter extends OncePerRequestFilter {

    /**
     * Intercepts the request, sets the provider context, and clears it after the chain completes.
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

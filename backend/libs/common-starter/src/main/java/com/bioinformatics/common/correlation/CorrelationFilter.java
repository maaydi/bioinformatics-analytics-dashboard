package com.bioinformatics.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var id = CorrelationContext.normalize(request.getHeader(CorrelationContext.HEADER));
        response.setHeader(CorrelationContext.HEADER, id);
        try (var ignored = CorrelationContext.open(id)) {
            chain.doFilter(request, response);
        }
    }
}

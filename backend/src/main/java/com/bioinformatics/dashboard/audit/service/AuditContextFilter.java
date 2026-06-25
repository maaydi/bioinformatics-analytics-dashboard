package com.bioinformatics.dashboard.audit.service;

import com.bioinformatics.dashboard.audit.dto.AuditWebDetails;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditContextFilter implements Filter {
    @Override
    /**
     * Populate the audit context with web request details for the current thread,
     * invoke the filter chain, and ensure the context is cleared afterwards.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param chain the filter chain
     * @throws IOException if an I/O error occurs during processing
     * @throws ServletException if the processing fails
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            var ipAddress = extractIpAddress(httpRequest);
            var method = httpRequest.getMethod();
            var endpoint = httpRequest.getRequestURI();
            AuditContextHolder.set(new AuditWebDetails(method, endpoint, ipAddress));
            try {
                chain.doFilter(request, response);
            } finally {
                AuditContextHolder.clear();
            }
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : ipHeaders) {
            var ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

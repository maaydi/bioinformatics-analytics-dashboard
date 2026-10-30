package com.bioinformatics.common.audit;

import com.bioinformatics.common.audit.service.AuditContextFilter;
import com.bioinformatics.common.audit.service.AuditContextHolder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditContextFilterTest {

    private final AuditContextFilter filter = new AuditContextFilter();

    @AfterEach
    void cleanUp() {
        AuditContextHolder.clear();
    }

    @Test
    void capturesRequestContextAndClearsItAfterTheChain() throws Exception {
        var request = new MockHttpServletRequest("POST", "/filters");
        request.addHeader("X-Forwarded-For", "192.0.2.1, 192.0.2.2");
        request.addHeader("X-Correlation-ID", "request-123");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var details = AuditContextHolder.get();
            assertThat(details.httpMethod()).isEqualTo("POST");
            assertThat(details.endpoint()).isEqualTo("/filters");
            assertThat(details.ipAddress()).isEqualTo("192.0.2.1");
            assertThat(details.correlationId()).isEqualTo("request-123");
        });

        assertThat(AuditContextHolder.get()).isNull();
    }

    @Test
    void clearsContextWhenRequestFailsAndPreservesOriginalException() {
        var request = new MockHttpServletRequest("GET", "/detail");
        request.setRemoteAddr("127.0.0.1");
        var original = new ServletException("request failure");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(AuditContextHolder.get().ipAddress()).isEqualTo("127.0.0.1");
            throw original;
        })).isSameAs(original);

        assertThat(AuditContextHolder.get()).isNull();
    }
}

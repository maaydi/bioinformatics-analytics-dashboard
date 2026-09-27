package com.bioinformatics.analyticsservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationPropertiesTest {

    @Test
    void createsValidProperties() {
        var properties = new ApplicationProperties(new ApplicationProperties.ViewRefresh(3, 1000, 250, 10_000));

        assertThat(properties.viewRefresh().maxAttempts()).isEqualTo(3);
        assertThat(properties.viewRefresh().perViewTimeoutMs()).isEqualTo(1000L);
        assertThat(properties.viewRefresh().retryBackoffMs()).isEqualTo(250L);
        assertThat(properties.viewRefresh().sequenceSlaMs()).isEqualTo(10_000L);
    }

    @Test
    void rejectsInvalidMaxAttempts() {
        assertThatThrownBy(() -> new ApplicationProperties(new ApplicationProperties.ViewRefresh(0, 1000, 0, 10_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.view-refresh.max-attempts must be >= 1");
    }

    @Test
    void rejectsInvalidTimeoutAndBackoffAndSla() {
        assertThatThrownBy(() -> new ApplicationProperties(new ApplicationProperties.ViewRefresh(3, 0, 0, 10_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.view-refresh.per-view-timeout-ms must be >= 1");

        assertThatThrownBy(() -> new ApplicationProperties(new ApplicationProperties.ViewRefresh(3, 1000, -1, 10_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.view-refresh.retry-backoff-ms must be >= 0");

        assertThatThrownBy(() -> new ApplicationProperties(new ApplicationProperties.ViewRefresh(3, 1000, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.view-refresh.sequence-sla-ms must be >= 1");
    }
}


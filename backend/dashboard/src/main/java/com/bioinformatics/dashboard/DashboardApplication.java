package com.bioinformatics.dashboard;

import com.bioinformatics.dashboard.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Bioinformatics Analytics Dashboard backend.
 *
 * <p>Architecture overview:
 * <ul>
 *   <li>{@code gene/}        — Protein/gene CRUD, search, and CSV export</li>
 *   <li>{@code analytics/}  — Materialized-view chart endpoints</li>
 *   <li>{@code admin/}      — Import job management (ADMIN only)</li>
 *   <li>{@code savedfilter/}— Saved filter sets per user</li>
 *   <li>{@code auth/}       — JWT login/refresh</li>
 *   <li>{@code batch/}      — Spring Batch UniProt import pipeline</li>
 *   <li>{@code security/}   — JWT filter, UserDetailsService</li>
 *   <li>{@code config/}     — SecurityConfig, WebConfig</li>
 *   <li>{@code exception/}  — Global exception handler</li>
 * </ul>
 *
 * @see <a href="../../../../../../../documentation/overview.md">overview.md</a>
 * @see <a href="../../../../../../../documentation/api-contract.md">api-contract.md</a>
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableAsync
public class DashboardApplication {

    static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}

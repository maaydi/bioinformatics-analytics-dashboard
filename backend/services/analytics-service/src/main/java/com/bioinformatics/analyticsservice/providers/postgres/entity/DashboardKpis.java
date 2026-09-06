package com.bioinformatics.analyticsservice.providers.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Immutable entity mapping to the pre-aggregated materialized view for dashboard KPIs.
 * Eliminates dynamic aggregation overhead on the primary gene tables.
 */
@Entity
@Immutable
@Table(name = "mv_dashboard_kpis")
@RequiredArgsConstructor
@Getter
public class DashboardKpis {
    @Id
    private Long totalProteins;
    private Long reviewedCount;
    private Integer organismCount;
    private Integer taxonCount;
    @Column(name = "avg_length", columnDefinition = "NUMERIC")
    private Integer avgLength;
    @Column(name = "avg_molecular_weight", columnDefinition = "NUMERIC")
    private Long avgMolecularWeight;
    private Integer minLength;
    private Integer maxLength;
}

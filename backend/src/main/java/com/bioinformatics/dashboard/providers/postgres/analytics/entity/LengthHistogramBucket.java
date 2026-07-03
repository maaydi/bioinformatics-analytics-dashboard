package com.bioinformatics.dashboard.providers.postgres.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Immutable entity mapping to the pre-bucketed length histogram materialized view.
 * Standardizes visualization scales and avoids heavy sequential math across millions of rows.
 */
@Entity
@Immutable
@Table(name = "mv_length_histogram")
@RequiredArgsConstructor
@Getter
public class LengthHistogramBucket {
    @Id
    private int bucket;

    private int rangeMin;
    private int rangeMax;
    private long count;
}

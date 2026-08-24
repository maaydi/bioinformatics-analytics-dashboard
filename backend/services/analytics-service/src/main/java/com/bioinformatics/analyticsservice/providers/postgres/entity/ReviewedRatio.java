package com.bioinformatics.analyticsservice.providers.postgres.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Immutable entity mapping to the Swiss-Prot/TrEMBL ratio materialized view.
 * Delivers immediate insight into data reliability scores across the database.
 */
@Entity
@Immutable
@Table(name = "mv_reviewed_ratio")
@RequiredArgsConstructor
@Getter
public class ReviewedRatio {
    @Id
    private Boolean reviewed;
    private Long count;
}

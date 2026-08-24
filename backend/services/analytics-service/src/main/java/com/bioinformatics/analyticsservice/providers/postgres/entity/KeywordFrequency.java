package com.bioinformatics.analyticsservice.providers.postgres.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Immutable entity mapping to the keyword frequency materialized view.
 * Solves performance bottleneck associated with complex dictionary frequency mapping.
 */
@Entity
@Table(name = "mv_keyword_frequency")
@Immutable
@RequiredArgsConstructor
@Getter
public class KeywordFrequency {
    @Id
    private String keyword;
    private Long count;

}

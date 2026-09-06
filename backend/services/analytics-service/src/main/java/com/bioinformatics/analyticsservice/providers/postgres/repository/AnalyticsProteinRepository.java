package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.common.gene.repository.ProteinEntryRepository;

/**
 * Defines dynamically evaluated analytics queries spanning multiple criteria.
 * Implementations should parse JPA specifications to render accurate runtime distributions.
 */
public interface AnalyticsProteinRepository extends ProteinEntryRepository, AnalyticsViewProteinRepository {
}

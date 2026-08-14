package com.bioinformatics.dashboard.providers.postgres.analytics.repository;

import com.bioinformatics.dashboard.providers.postgres.analytics.entity.EvidenceDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDistributionRepository extends JpaRepository<EvidenceDistribution, Short> {
}

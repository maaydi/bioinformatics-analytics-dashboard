package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.EvidenceDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDistributionRepository extends JpaRepository<EvidenceDistribution, Short> {
}

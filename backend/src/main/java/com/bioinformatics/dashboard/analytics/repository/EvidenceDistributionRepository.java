package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.EvidenceDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDistributionRepository extends JpaRepository<EvidenceDistribution, Short> {
}

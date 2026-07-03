package com.bioinformatics.dashboard.providers.postgres.analytics.repository;

import com.bioinformatics.dashboard.providers.postgres.analytics.entity.ReviewedRatio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewedRatioRepository extends JpaRepository<ReviewedRatio, Boolean> {
}

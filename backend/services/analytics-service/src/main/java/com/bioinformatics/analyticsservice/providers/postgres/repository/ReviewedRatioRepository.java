package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.ReviewedRatio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewedRatioRepository extends JpaRepository<ReviewedRatio, Boolean> {
}

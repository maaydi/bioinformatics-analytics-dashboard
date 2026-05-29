package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.ReviewedRatio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewedRatioRepository extends JpaRepository<ReviewedRatio, Boolean> {
}

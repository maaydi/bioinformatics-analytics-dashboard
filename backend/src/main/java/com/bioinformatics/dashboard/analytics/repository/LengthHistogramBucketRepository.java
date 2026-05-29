package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.LengthHistogramBucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LengthHistogramBucketRepository extends JpaRepository<LengthHistogramBucket, Integer> {
    List<LengthHistogramBucket> findAllByOrderByBucketAsc();
}

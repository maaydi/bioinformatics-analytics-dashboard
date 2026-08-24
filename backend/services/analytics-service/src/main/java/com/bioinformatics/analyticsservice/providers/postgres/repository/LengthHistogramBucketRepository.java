package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.LengthHistogramBucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LengthHistogramBucketRepository extends JpaRepository<LengthHistogramBucket, Integer> {
    List<LengthHistogramBucket> findAllByOrderByBucketAsc();
}

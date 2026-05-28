package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.LengthHistogramBucket;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface LengthHistogramBucketRepository extends Repository<LengthHistogramBucket, Integer> {
    List<LengthHistogramBucket> findAllByOrderByBucketAsc();
}

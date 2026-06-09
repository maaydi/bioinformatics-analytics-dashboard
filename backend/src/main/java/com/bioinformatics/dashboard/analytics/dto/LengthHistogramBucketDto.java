package com.bioinformatics.dashboard.analytics.dto;

public record LengthHistogramBucketDto(int bucket,
                                       long rangeMin,
                                       long rangeMax,
                                       long count) {
    /**
     * Constructor for Analytic Protein Repository implementation use
     *
     */
    public LengthHistogramBucketDto(Integer bucket, Long count) {
        this(
                bucket,
                (bucket - 1) * 100L,
                bucket * 100L,
                count
        );
    }
}

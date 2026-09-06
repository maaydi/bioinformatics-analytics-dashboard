package com.bioinformatics.analyticsservice.models;

/**
 * Defines a discrete length interval and its corresponding protein frequency.
 */
public record LengthHistogramBucketDto(int bucket,
                                       long rangeMin,
                                       long rangeMax,
                                       long count) {
    /**
     * Initializes bucket boundaries uniformly (e.g., 500-unit intervals).
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

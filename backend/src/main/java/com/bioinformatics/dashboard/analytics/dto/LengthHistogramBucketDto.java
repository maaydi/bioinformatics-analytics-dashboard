package com.bioinformatics.dashboard.analytics.dto;

public record LengthHistogramBucketDto(int bucket,
                                       long rangeMin,
                                       long rangeMax,
                                       long count) {
}

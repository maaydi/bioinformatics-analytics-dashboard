package com.bioinformatics.dashboard.analytics.dto;

public record LengthBucketDto(int bucket,
                              long rangeMin,
                              long rangeMax,
                              long count) {
}

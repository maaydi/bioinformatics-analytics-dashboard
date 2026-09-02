package com.bioinformatics.analyticsservice.materializeviews.dto;

public record ViewToRefresh(String viewName, boolean concurrently) {
}

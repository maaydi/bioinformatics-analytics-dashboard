package com.bioinformatics.dashboard.savedfilter.dto;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;

import java.time.Instant;

public record SavedFilterDto(long id, String name, GeneSearchRequest filterJson, Instant createdAt) {
}

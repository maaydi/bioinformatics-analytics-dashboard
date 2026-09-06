package com.bioinformatics.common.models.filter;

import com.bioinformatics.common.models.gene.GeneSearchRequest;

import java.time.Instant;

public record SavedFilterDto(long id, String name, GeneSearchRequest filterJson, Instant createdAt) {
}

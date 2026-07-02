package com.bioinformatics.dashboard.providers.postgres.analytics.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key defining the taxonomy uniqueness constraint (combines Name and TaxID).
 */
@RequiredArgsConstructor
@Getter
@Setter
public class OrganismCountId implements Serializable {
    private String organismName;
    private Integer taxid;
}

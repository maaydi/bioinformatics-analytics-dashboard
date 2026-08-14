package com.bioinformatics.dashboard.providers.postgres.analytics.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key defining the taxonomy uniqueness constraint (combines Name and TaxID).
 */
@RequiredArgsConstructor
@Getter
@Setter
public class OrganismCountId implements Serializable {
    private String organismName;
    private Integer taxid;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrganismCountId that = (OrganismCountId) o;
        return Objects.equals(organismName, that.organismName) && Objects.equals(taxid, that.taxid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organismName, taxid);
    }
}

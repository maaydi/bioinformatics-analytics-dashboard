package com.bioinformatics.dashboard.model.gene;

/**
 * Host organism record for viral or pathogenic proteins.
 * Records the organism(s) that a protein infects or is associated with.
 */
public record HostOrganismDto(long id, int taxid, String name) {
}

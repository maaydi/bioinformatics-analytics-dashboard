package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import com.bioinformatics.dashboard.model.uniprot.dto.GeneName;

/**
 * A lightweight representation of gene information associated with a protein.
 *
 * <p>Contains a single {@link GeneName} object with the gene's primary and alternative names.
 * Used in summary contexts where full gene details are unnecessary.</p>
 *
 * @param geneName the gene name(s) and associated metadata
 */
public record GeneLight(GeneName geneName) {
}

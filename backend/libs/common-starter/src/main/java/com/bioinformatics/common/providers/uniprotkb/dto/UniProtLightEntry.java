package com.bioinformatics.common.providers.uniprotkb.dto;


import com.bioinformatics.common.uniprot.dto.ProteinDescription;

import java.util.List;

/**
 * A lightweight projection of a UniProt KB protein entry.
 *
 * <p>Contains only essential protein metadata, suitable for search result lists and summary views
 * where the full UniProtEntry would be excessive. Omits detailed sequences, annotations,
 * references, and cross-references except as summarized by features and genes.</p>
 *
 * @param primaryAccession   Protein primary accession
 * @param uniProtkbId        the UniProt accession number (primary identifier)
 * @param features           lightweight feature annotations (type only, without detailed positions)
 * @param genes              genes associated with this protein
 * @param proteinDescription high-level protein name and classification
 */
public record UniProtLightEntry(String entryType, String primaryAccession, String uniProtkbId,
                                List<FeatureLight> features,
                                List<GeneLight> genes,
                                ProteinDescription proteinDescription) {
}

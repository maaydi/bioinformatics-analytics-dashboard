package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publication record citing evidence for protein annotations and data.
 * Contains PubMed ID and bibliographic details for scientific literature references.
 */
@Builder
public record ProteinPublicationDto(

        Long id,

        Short refNumber,

        String pubmedId,

        String doi,

        String authors,

        String title,

        String journal

) implements ExportFormatSerializable {
    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }

    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("Ref Number", format(refNumber));
            put("Pubmed Id", format(pubmedId));
            put("Doi", format(doi));
            put("Authors", format(authors));
            put("Title", format(title));
            put("Journal", format(journal));
        }};
    }
}

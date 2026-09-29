package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Publication record citing evidence for protein annotations and data.
 * Contains PubMed ID and bibliographic details for scientific literature references.
 */
@Builder
public record ProteinPublicationDto(

        @ExportIgnore
        Long id,

        @ExportField(name = "refNumber", displayName = "Reference Number", description = "Internal reference number for the publication")
        Short refNumber,

        @ExportField(name = "pubmedId", displayName = "PubMed ID", description = "PubMed unique identifier for the publication")
        String pubmedId,

        @ExportField(name = "doi", displayName = "DOI", description = "Digital Object Identifier for the publication")
        String doi,

        @ExportField(name = "authors", displayName = "Authors", description = "List of authors of the publication")
        String authors,

        @ExportField(name = "title", displayName = "Title", description = "Title of the publication")
        String title,

        @ExportField(name = "journal", displayName = "Journal", description = "Journal name and publication details")
        String journal

) implements ExportFormatSerializable {

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

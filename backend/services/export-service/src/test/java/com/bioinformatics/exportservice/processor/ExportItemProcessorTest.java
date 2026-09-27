package com.bioinformatics.exportservice.processor;

import com.bioinformatics.common.gene.dto.CrossReferenceDto;
import com.bioinformatics.common.gene.dto.GoTermDto;
import com.bioinformatics.common.gene.dto.HostOrganismDto;
import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.exportservice.batch.ExportJobParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportItemProcessorTest {

    @Mock
    private ExportJobParameters parameters;

    @InjectMocks
    private ProteinDetailProcessor processor;

    @Test
    void process_extractsSelectedFields() {
        when(parameters.getExportedFields()).thenReturn(List.of("accession", "geneNamePrimary", "keywords"));

        var dto = ProteinDetailDto.builder()
                .accession("P12345")
                .geneNamePrimary("BRCA1")
                .sequence("MKT")
                .evidenceLevel((short) 1)
                .keywords(List.of("DNA repair", "cancer"))
                .build();

        Map<String, Object> result = processor.process(dto);

        assertThat(result).containsOnlyKeys("accession", "geneNamePrimary", "keywords");
        assert result != null;
        assertThat(result.get("accession")).isEqualTo("\"P12345\"");
        assertThat(result.get("geneNamePrimary")).isEqualTo("\"BRCA1\"");
        assertThat(result.get("keywords")).isEqualTo("\"DNA repair; cancer\"");
    }

    @Test
    void process_handlesNullCollections() {
        when(parameters.getExportedFields()).thenReturn(List.of("keywords", "features", "comments"));

        var dto = ProteinDetailDto.builder()
                .keywords(null)
                .features(null)
                .comments(null)
                .evidenceLevel((short) 1)
                .build();

        var result = processor.process(dto);

        assertThat(result).containsOnlyKeys("keywords", "features", "comments");
        assert result != null;
        assertThat(result.get("keywords")).isEqualTo("\"\"");
        assertThat(result.get("features")).isEqualTo("\"\"");
        assertThat(result.get("comments")).isEqualTo("\"\"");
    }

    @Test
    void process_mapsNestedObjects() {
        when(parameters.getExportedFields()).thenReturn(List.of("crossReferences", "hostOrganisms", "goTerms"));

        var dto = ProteinDetailDto.builder()
                .crossReferences(Set.of(new CrossReferenceDto(0L, "RefSeq", "NP_001", null, null)))
                .hostOrganisms(Set.of(new HostOrganismDto(0L, 9606, "Homo sapiens")))
                .goTerms(Set.of(new GoTermDto(0, "GO:0008150", 'P', null)))
                .evidenceLevel((short) 1)
                .build();

        var result = processor.process(dto);

        assert result != null;
        assertThat(result.get("crossReferences")).isEqualTo("\"RefSeq:NP_001\"");
        assertThat(result.get("hostOrganisms")).isEqualTo("\"Homo sapiens\"");
        assertThat(result.get("goTerms")).isEqualTo("\"GO:0008150\"");
    }
}

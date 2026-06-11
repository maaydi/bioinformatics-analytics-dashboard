package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ExportRowCapExceededException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneServiceTest {

    @Mock
    ProteinEntryService proteinEntryService;

    @Mock
    KeywordRepository keywordRepository;

    @Mock
    GeneMapper mapper;

    AppProperties appProperties;

    GeneService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        var export = appProperties.getExport();
        export.setCsv(new AppProperties.Csv());
        export.getCsv().setMaxRows(1000);

        service = new GeneService(proteinEntryService, keywordRepository, mapper, appProperties);
    }

    private GeneSearchRequest buildRequest(
            List<String> keywords,
            Integer taxid,
            String lineage,
            Integer size,
            String sort
    ) {
        return new GeneSearchRequest(
                null, // globalSearch
                null, // accession
                null, // entryName
                null, // geneNamePrimary
                null, // proteinFullName
                null, // reviewed
                null, // organism
                taxid, // taxid
                lineage, // lineage
                null, // lengthMin
                null, // lengthMax
                null, // molecularWeightMin
                null, // molecularWeightMax
                null, // evidenceLevels
                keywords, // keywords
                null, // goTermId
                null, // goAspect
                null, // featureType
                null, // crossRefSource
                0, // page
                size, // size
                sort, // sort
                null // direction
        );
    }

    @Test
    void listGenes_returnsPage() {
        var entry = new ProteinEntry();
        entry.setId(1L);

        var page = new PageImpl<>(List.of(entry), PageRequest.of(0, 10), 1);
        when(proteinEntryService.findAll(any(PageRequest.class))).thenReturn(page);

        var dto = new ProteinSummaryDto(1L, "ACC", "entry", "full", "gene", "org", 123, true, 100, 200, (short) 1, List.of());
        when(mapper.toSummary(entry)).thenReturn(dto);

        PagedResponse<ProteinSummaryDto> result = service.listGenes(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(dto, result.content().getFirst());
    }

    @Test
    void getGeneById_found() {
        var entry = new ProteinEntry();
        entry.setId(2L);

        when(proteinEntryService.findAdditionalDetails(2L)).thenReturn(Optional.of(entry));

        var detail = new ProteinDetailDto(
                2L,
                "ACC", "entry", false,
                null, null, null, null, null,
                null, null, null,
                null, new String[0], new String[0], new String[0],
                null, null, null, new String[0],
                null, null, null, null,
                null,
                null,
                null, null,
                List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
        );

        when(mapper.toDetail(entry)).thenReturn(detail);

        var res = service.getGeneById(2L);
        assertEquals(detail, res);
    }

    @Test
    void getGeneById_notFound_throws() {
        when(proteinEntryService.findAdditionalDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGeneById(99L));
    }

    @Test
    void assertWithinExportLimit_exceedsLimit_throws() {
        appProperties.getExport().getCsv().setMaxRows(4);

        var request = buildRequest(null, null, null, 10, null);

        when(proteinEntryService.count(ArgumentMatchers.any())).thenReturn(5L);

        assertThrows(ExportRowCapExceededException.class, () -> service.assertWithinExportLimit(request));
    }

    @Test
    void assertWithinExportLimit_return_size() {
        appProperties.getExport().getCsv().setMaxRows(4);

        var request = buildRequest(null, null, null, 10, null);

        when(proteinEntryService.count(ArgumentMatchers.any())).thenReturn(3L);

        var count = service.assertWithinExportLimit(request);
        assertEquals(3L, count);
    }

    @Test
    void searchGenes_withKeywords_filters() {
        var entry = new ProteinEntry();
        entry.setId(10L);

        var page = new PageImpl<>(List.of(entry), PageRequest.of(0, 10), 1);
        when(proteinEntryService.findAll(ArgumentMatchers.<Specification<ProteinEntry>>any(), any(Pageable.class))).thenReturn(page);

        var dto = new ProteinSummaryDto(10L, "ACC10", "entry10", "full10",
                "gene10", "org10", 100, true, 50, 60,
                (short) 1, List.of("kw"));
        when(mapper.toSummary(entry)).thenReturn(dto);

        var request = buildRequest(List.of("kw"), null, null, 10, null);
        var res = service.searchGenes(request);

        assertNotNull(res);
        assertEquals(1, res.content().size());
        assertEquals(dto, res.content().getFirst());
    }

    @Test
    void searchGenes_lineage_filters() {
        var entry = new ProteinEntry();
        entry.setId(11L);

        var page = new PageImpl<>(List.of(entry), PageRequest.of(0, 10), 1);
        when(proteinEntryService.findAll(ArgumentMatchers.<Specification<ProteinEntry>>any(), any(Pageable.class))).thenReturn(page);

        var dto = new ProteinSummaryDto(11L, "ACC11", "entry11", "full11",
                "gene11", "org11", 101, false, 70, 80,
                (short) 2, List.of());
        when(mapper.toSummary(entry)).thenReturn(dto);

        var request = buildRequest(null, 111, "Bacteria", 10, null);
        var res = service.searchGenes(request);

        assertNotNull(res);
        assertEquals(1, res.content().size());
        assertEquals(dto, res.content().getFirst());
    }

    @Test
    void requestPage_invalidSort_throws() {
        var request = buildRequest(null, null, null, 10, "badSort");
        var allowed = Set.of("id");
        assertThrows(IllegalArgumentException.class, () -> request.getRequestPage(allowed, "id"));
    }

    @Test
    void exportCsv_respectsLimit_noThrow() throws Exception {
        appProperties.getExport().getCsv().setMaxRows(2);

        var entry1 = new ProteinEntry();
        entry1.setId(21L);
        var entry2 = new ProteinEntry();
        entry2.setId(22L);

        var page = new PageImpl<>(List.of(entry1, entry2), PageRequest.of(0, 2), 2);
        when(proteinEntryService.findAll(ArgumentMatchers.<Specification<ProteinEntry>>any(), any(Pageable.class))).thenReturn(page);

        var dto1 = new ProteinSummaryDto(21L, "ACC21", "e21", "f21",
                "g21", "o21", 201, true, 10, 20,
                (short) 1, List.of());
        var dto2 = new ProteinSummaryDto(22L, "ACC22", "e22", "f22",
                "g22", "o22", 202, true, 11, 21,
                (short) 1, List.of());
        when(mapper.toSummary(entry1)).thenReturn(dto1);
        when(mapper.toSummary(entry2)).thenReturn(dto2);

        var request = buildRequest(null, null, null, 2, null);
        var writer = new StringWriter();
        service.exportCsv(request, writer, 2);
        var output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("accession") || output.contains("ACC21"));
    }
}



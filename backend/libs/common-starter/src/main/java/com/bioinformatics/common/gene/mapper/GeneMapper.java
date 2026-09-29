package com.bioinformatics.common.gene.mapper;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.gene.entity.Keyword;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper used to project {@link ProteinEntry} persistence entities to API-facing DTOs.
 *
 * <p>This layer ensures the REST API never exposes JPA entities directly and keeps the transformation
 * logic in one explicit place. The keyword conversion helper is intentionally simple and deterministic.
 */
@Mapper(componentModel = "spring")
public interface GeneMapper {

    @Mapping(target = "keywords", source = "keywords", qualifiedByName = "keywordsToNames")
    ProteinSummaryDto toSummary(ProteinEntry entity);

    @Mapping(target = "keywords", source = "keywords", qualifiedByName = "keywordsToNames")
    ProteinDetailDto toDetail(ProteinEntry entity);

    @Named("keywordsToNames")
    static List<String> keywordsToNames(List<Keyword> keywords) {
        if (keywords == null) return List.of();
        return keywords.stream().map(Keyword::getName).toList();
    }

}

package com.bioinformatics.dashboard.gene.mapper;

import com.bioinformatics.dashboard.gene.entity.Keyword;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper — {@link ProteinEntry} entity ↔ DTOs.
 *
 * <p>Entities must never be returned directly from controllers.
 * This mapper is the only place where entity fields are projected to DTO fields.
 *
 * @see <a href="{@docRoot}/documentation/constitution.md">Backend Standards — DTOs for API contracts</a>
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

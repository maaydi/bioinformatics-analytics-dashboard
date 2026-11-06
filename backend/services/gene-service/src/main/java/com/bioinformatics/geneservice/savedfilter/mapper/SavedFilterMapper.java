package com.bioinformatics.geneservice.savedfilter.mapper;

import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.geneservice.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.geneservice.savedfilter.entity.SavedFilter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavedFilterMapper {

    SavedFilterDto toDto(SavedFilter entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "owner", source = "owner")
    SavedFilter toEntity(SavedFilterCreateRequest request, String owner);
}

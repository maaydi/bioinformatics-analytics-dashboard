package com.bioinformatics.dashboard.providers.postgres.savedfilter.mapper;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.model.savedfilter.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.model.savedfilter.SavedFilterDto;
import com.bioinformatics.dashboard.providers.postgres.savedfilter.entity.SavedFilter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavedFilterMapper {

    SavedFilterDto toDto(SavedFilter entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "owner", source = "owner")
    SavedFilter toEntity(SavedFilterCreateRequest request, AppUser owner);
}

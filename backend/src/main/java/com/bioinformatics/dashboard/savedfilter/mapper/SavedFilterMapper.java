package com.bioinformatics.dashboard.savedfilter.mapper;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavedFilterMapper {

    SavedFilterDto toDto(SavedFilter entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", source = "owner")
    SavedFilter toEntity(SavedFilterCreateRequest request, AppUser owner);
}

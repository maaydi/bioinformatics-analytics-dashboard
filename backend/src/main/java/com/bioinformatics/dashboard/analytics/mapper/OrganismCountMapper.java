package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.dto.OrganismCountDto;
import com.bioinformatics.dashboard.analytics.entity.OrganismCount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganismCountMapper {

    OrganismCountDto toDto(OrganismCount entity);


}

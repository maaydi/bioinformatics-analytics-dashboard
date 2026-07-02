package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.entity.OrganismCount;
import com.bioinformatics.dashboard.model.analytics.OrganismCountDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganismCountMapper {

    OrganismCountDto toDto(OrganismCount entity);


}

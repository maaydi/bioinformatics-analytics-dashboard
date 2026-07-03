package com.bioinformatics.dashboard.providers.postgres.analytics.mapper;

import com.bioinformatics.dashboard.model.analytics.OrganismCountDto;
import com.bioinformatics.dashboard.providers.postgres.analytics.entity.OrganismCount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganismCountMapper {

    OrganismCountDto toDto(OrganismCount entity);


}

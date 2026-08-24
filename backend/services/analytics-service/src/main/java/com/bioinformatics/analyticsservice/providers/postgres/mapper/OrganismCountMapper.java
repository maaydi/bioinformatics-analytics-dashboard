package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.OrganismCountDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.OrganismCount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganismCountMapper {

    OrganismCountDto toDto(OrganismCount entity);


}

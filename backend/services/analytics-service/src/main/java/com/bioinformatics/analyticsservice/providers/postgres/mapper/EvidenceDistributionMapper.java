package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.EvidenceDistributionDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.EvidenceDistribution;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvidenceDistributionMapper {


    EvidenceDistributionDto toDto(EvidenceDistribution entity);


}

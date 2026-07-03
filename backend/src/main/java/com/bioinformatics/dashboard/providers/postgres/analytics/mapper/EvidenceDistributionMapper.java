package com.bioinformatics.dashboard.providers.postgres.analytics.mapper;

import com.bioinformatics.dashboard.model.analytics.EvidenceDistributionDto;
import com.bioinformatics.dashboard.providers.postgres.analytics.entity.EvidenceDistribution;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvidenceDistributionMapper {


    EvidenceDistributionDto toDto(EvidenceDistribution entity);


}

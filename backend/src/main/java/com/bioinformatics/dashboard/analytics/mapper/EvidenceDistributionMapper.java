package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.entity.EvidenceDistribution;
import com.bioinformatics.dashboard.model.analytics.EvidenceDistributionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvidenceDistributionMapper {


    EvidenceDistributionDto toDto(EvidenceDistribution entity);


}

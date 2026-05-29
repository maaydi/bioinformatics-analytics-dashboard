package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.dto.EvidenceDistributionDto;
import com.bioinformatics.dashboard.analytics.entity.EvidenceDistribution;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvidenceDistributionMapper {


    EvidenceDistributionDto toDto(EvidenceDistribution entity);


}

package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.LengthHistogramBucketDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.LengthHistogramBucket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LengthHistogramBucketMapper {

    LengthHistogramBucketDto toDto(LengthHistogramBucket entity);


}

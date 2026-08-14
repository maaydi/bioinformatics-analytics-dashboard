package com.bioinformatics.dashboard.providers.postgres.analytics.mapper;

import com.bioinformatics.dashboard.model.analytics.LengthHistogramBucketDto;
import com.bioinformatics.dashboard.providers.postgres.analytics.entity.LengthHistogramBucket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LengthHistogramBucketMapper {

    LengthHistogramBucketDto toDto(LengthHistogramBucket entity);


}

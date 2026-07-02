package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.entity.LengthHistogramBucket;
import com.bioinformatics.dashboard.model.analytics.LengthHistogramBucketDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LengthHistogramBucketMapper {

    LengthHistogramBucketDto toDto(LengthHistogramBucket entity);


}

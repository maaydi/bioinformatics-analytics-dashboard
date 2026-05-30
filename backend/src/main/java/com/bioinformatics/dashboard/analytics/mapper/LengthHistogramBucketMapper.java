package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.dto.LengthHistogramBucketDto;
import com.bioinformatics.dashboard.analytics.entity.LengthHistogramBucket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LengthHistogramBucketMapper {

    LengthHistogramBucketDto toDto(LengthHistogramBucket entity);


}

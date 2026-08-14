package com.bioinformatics.dashboard.providers.postgres.analytics.mapper;

import com.bioinformatics.dashboard.model.analytics.ReviewedRatioDto;
import com.bioinformatics.dashboard.providers.postgres.analytics.entity.ReviewedRatio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewedRatioMapper {

    ReviewedRatioDto toDto(ReviewedRatio entity);


}

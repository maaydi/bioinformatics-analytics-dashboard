package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.dto.ReviewedRatioDto;
import com.bioinformatics.dashboard.analytics.entity.ReviewedRatio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewedRatioMapper {

    ReviewedRatioDto toDto(ReviewedRatio entity);


}

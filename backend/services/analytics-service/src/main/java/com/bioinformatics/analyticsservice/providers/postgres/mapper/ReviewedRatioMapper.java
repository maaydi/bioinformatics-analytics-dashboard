package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.ReviewedRatioDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.ReviewedRatio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewedRatioMapper {

    ReviewedRatioDto toDto(ReviewedRatio entity);


}

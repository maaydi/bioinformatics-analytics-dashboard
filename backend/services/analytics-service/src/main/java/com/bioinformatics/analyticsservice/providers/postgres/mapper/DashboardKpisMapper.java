package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.DashboardKpisDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.DashboardKpis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DashboardKpisMapper {

    @Mapping(target = "unreviewedCount", expression = "java(calculateUnreviewedCount(entity))")
    DashboardKpisDto toDto(DashboardKpis entity);

    default long calculateUnreviewedCount(DashboardKpis entity) {
        return entity.getTotalProteins() - entity.getReviewedCount();
    }


}

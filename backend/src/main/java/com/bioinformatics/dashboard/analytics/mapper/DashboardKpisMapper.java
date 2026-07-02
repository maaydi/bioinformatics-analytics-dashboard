package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.entity.DashboardKpis;
import com.bioinformatics.dashboard.model.analytics.DashboardKpisDto;
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

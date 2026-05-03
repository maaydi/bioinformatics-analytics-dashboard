package com.bioinformatics.dashboard.job.mapper;

import java.time.Duration;
import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.entity.ImportJob;

@Mapper(componentModel = "spring")
public interface ImportJobMapper {

    ImportJobSummary toSummary(ImportJob entity);

    @Mapping(target = "progressPercent", expression = "java(calculateProgress(entity))")
    @Mapping(target = "elapsedMs", expression = "java(calculateElapsedTime(entity))")
    ImportJobProgress toJobProgress(ImportJob entity);

    default int calculateProgress(ImportJob entity) {
        if (entity.getTotalEstimated() <= 0)
            return 0;
        return (int) ((double) entity.getRecordsProcessed() / entity.getTotalEstimated() * 100);
    }

    default long calculateElapsedTime(ImportJob entity) {
        if (entity.getCreatedAt() == null) {
            return 0L;
        }
        return Duration.between(entity.getCreatedAt(), Instant.now()).toMillis();
    }

}

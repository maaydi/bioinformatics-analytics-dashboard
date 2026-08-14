package com.bioinformatics.dashboard.job.mapper;

import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Duration;
import java.time.Instant;

@Mapper(componentModel = "spring")
public interface ImportJobMapper {

    @Mapping(target = "progressPercent", expression = "java(calculateProgress(entity))")
    ImportJobSummary toSummary(ImportJob entity);

    @Mapping(target = "progressPercent", expression = "java(calculateProgress(entity))")
    @Mapping(target = "elapsedMs", expression = "java(calculateElapsedTime(entity))")
    ImportJobProgress toJobProgress(ImportJob entity);

    default int calculateProgress(ImportJob entity) {
        if (entity.getTotalEstimated() <= 0)
            return 0;
        if (entity.getStatus() == ImportStatus.COMPLETED) {
            return (int) ((double) entity.getEntryCount() / entity.getTotalEstimated() * 100);
        }
        return (int) ((double) entity.getRecordsProcessed() / entity.getTotalEstimated() * 100);
    }

    default long calculateElapsedTime(ImportJob entity) {
        if (entity.getCreatedAt() == null) {
            return 0L;
        }
        return Duration.between(entity.getCreatedAt(), Instant.now()).toMillis();
    }

}

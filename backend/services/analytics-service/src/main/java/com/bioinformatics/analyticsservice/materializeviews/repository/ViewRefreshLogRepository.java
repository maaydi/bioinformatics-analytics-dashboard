package com.bioinformatics.analyticsservice.materializeviews.repository;

import com.bioinformatics.analyticsservice.materializeviews.entity.ViewRefreshLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewRefreshLogRepository extends JpaRepository<ViewRefreshLog, Long> {

}

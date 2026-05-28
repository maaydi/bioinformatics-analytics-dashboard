package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.DashboardKpis;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface DashboardKpisRepository extends Repository<DashboardKpis, Long> {
    Optional<DashboardKpis> findFirstBy();
}

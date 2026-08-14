package com.bioinformatics.dashboard.providers.postgres.analytics.repository;

import com.bioinformatics.dashboard.providers.postgres.analytics.entity.DashboardKpis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardKpisRepository extends JpaRepository<DashboardKpis, Long> {
    Optional<DashboardKpis> findFirstBy();
}

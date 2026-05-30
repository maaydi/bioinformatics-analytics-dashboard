package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.DashboardKpis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardKpisRepository extends JpaRepository<DashboardKpis, Long> {
    Optional<DashboardKpis> findFirstBy();
}

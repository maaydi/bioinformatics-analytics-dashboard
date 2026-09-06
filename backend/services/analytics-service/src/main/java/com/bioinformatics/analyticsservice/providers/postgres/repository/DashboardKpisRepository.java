package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.DashboardKpis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardKpisRepository extends JpaRepository<DashboardKpis, Long> {
    Optional<DashboardKpis> findFirstBy();
}

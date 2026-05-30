package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.OrganismCount;
import com.bioinformatics.dashboard.analytics.entity.OrganismCountId;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganismCountRepository extends JpaRepository<OrganismCount, OrganismCountId> {
    @Query("select o from OrganismCount o ORDER BY o.total DESC")
    List<OrganismCount> findAll(Limit limit);
}

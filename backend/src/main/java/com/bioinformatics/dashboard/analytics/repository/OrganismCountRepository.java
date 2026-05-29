package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.OrganismCount;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganismCountRepository extends JpaRepository<OrganismCount, String> {
    @Query("select o from OrganismCount o")
    List<OrganismCount> findAll(Limit limit);
}

package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.OrganismCount;
import com.bioinformatics.analyticsservice.providers.postgres.entity.OrganismCountId;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganismCountRepository extends JpaRepository<OrganismCount, OrganismCountId> {
    @Query("select o from OrganismCount o ORDER BY o.total DESC")
    List<OrganismCount> findAll(Limit limit);
}

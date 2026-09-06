package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.providers.postgres.entity.KeywordFrequency;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KeywordFrequencyRepository extends JpaRepository<KeywordFrequency, String> {
    @Query("select o from KeywordFrequency o ORDER BY o.count DESC")
    List<KeywordFrequency> findAll(Limit limit);
}

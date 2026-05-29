package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.entity.KeywordFrequency;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KeywordFrequencyRepository extends JpaRepository<KeywordFrequency, String> {
    @Query("select o from KeywordFrequency o")
    List<KeywordFrequency> findAll(Limit limit);
}

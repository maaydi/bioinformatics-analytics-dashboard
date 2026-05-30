package com.bioinformatics.dashboard.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "mv_keyword_frequency")
@Immutable
@RequiredArgsConstructor
@Getter
public class KeywordFrequency {
    @Id
    private String keyword;
    private Long count;

}

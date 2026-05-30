package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for {@code keyword} table.
 * Shared vocabulary mapped from KW lines in UniProt .dat files.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Domain Model §2</a>
 */
@Entity
@Table(name = "keyword")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "keyword_seq")
    @SequenceGenerator(name = "keyword_seq", sequenceName = "keyword_seq", allocationSize = 500)
    private Integer id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String name;
}

package com.bioinformatics.common.gene.entity;

import jakarta.persistence.*;
import lombok.*;

import static com.bioinformatics.shared.models.db.DbSchema.GENES_SCHEMA;

/**
 * JPA entity for {@code keyword} table.
 * Shared vocabulary mapped from KW lines in UniProt .dat files.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Domain Model §2</a>
 */
@Entity
@Table(schema = GENES_SCHEMA, name = "keyword")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "keyword_seq")
    @SequenceGenerator(name = "keyword_seq",schema = GENES_SCHEMA, sequenceName = "keyword_seq", allocationSize = 500)
    private Integer id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String name;
}

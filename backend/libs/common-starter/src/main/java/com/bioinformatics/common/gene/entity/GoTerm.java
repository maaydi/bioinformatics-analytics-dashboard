package com.bioinformatics.common.gene.entity;

import jakarta.persistence.*;
import lombok.*;

import static com.bioinformatics.shared.models.db.DbSchema.GENES_SCHEMA;

/**
 * JPA entity for {@code go_term} table.
 * Gene Ontology terms parsed from DR GO lines.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Domain Model §4</a>
 */
@Entity
@Table(schema = GENES_SCHEMA, name = "go_term")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "go_term_seq")
    @SequenceGenerator(name = "go_term_seq", sequenceName = "go_term_seq", allocationSize = 500)
    private Integer id;

    @Column(name = "go_id", nullable = false, unique = true, columnDefinition = "TEXT")
    private String goId;

    /** P = Biological Process, F = Molecular Function, C = Cellular Component */
    @Column(nullable = false, length = 1)
    private Character aspect;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
}

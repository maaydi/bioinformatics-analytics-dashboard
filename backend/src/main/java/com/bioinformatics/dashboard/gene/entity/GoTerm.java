package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for {@code go_term} table.
 * Gene Ontology terms parsed from DR GO lines.
 *
 * @see documentation/domain-model.md §4
 */
@Entity
@Table(name = "go_term")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "go_id", nullable = false, unique = true, length = 15)
    private String goId;

    /** P = Biological Process, F = Molecular Function, C = Cellular Component */
    @Column(nullable = false, length = 1)
    private Character aspect;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
}

package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for {@code protein_entry} table.
 *
 * <p>DDL is authoritative in documentation/domain-model.md §1.
 * Do NOT modify column names without updating the Flyway migration AND domain-model.md.
 *
 * <p>Relationships are lazily loaded to avoid N+1 — use {@code @EntityGraph} or
 * JOIN FETCH in the repository for detail queries (see domain-model.md design goals).
 */
@Entity
@Table(name = "protein_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProteinEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identification ────────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 20)
    private String accession;

    @Column(name = "entry_name", nullable = false, length = 50)
    private String entryName;

    @Column(nullable = false)
    private Boolean reviewed = false;

    // ── Dates ─────────────────────────────────────────────────────────────────
    @Column(name = "integrated_date")
    private LocalDate integratedDate;

    @Column(name = "sequence_date")
    private LocalDate sequenceDate;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "sequence_version")
    private Short sequenceVersion;

    @Column(name = "entry_version")
    private Short entryVersion;

    // ── Protein Name ──────────────────────────────────────────────────────────
    @Column(name = "protein_full_name", columnDefinition = "TEXT")
    private String proteinFullName;

    @Column(name = "protein_short_name", length = 200)
    private String proteinShortName;

    @Column(name = "protein_ec_number", length = 50)
    private String proteinEcNumber;

    // ── Gene Name ─────────────────────────────────────────────────────────────
    @Column(name = "gene_name_primary", length = 100)
    private String geneNamePrimary;

    @Column(name = "gene_name_synonyms", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] geneNameSynonyms;

    @Column(name = "gene_orf_names", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] geneOrfNames;

    @Column(name = "gene_ordered_locus", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] geneOrderedLocus;

    // ── Organism ──────────────────────────────────────────────────────────────
    @Column(name = "organism_name", nullable = false, length = 300)
    private String organismName;

    @Column(name = "organism_common_name", length = 150)
    private String organismCommonName;

    @Column(nullable = false)
    private Integer taxid;

    @Column(columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] lineage;

    // ── Sequence ──────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private Integer length;

    @Column(name = "molecular_weight")
    private Integer molecularWeight;

    @Column(name = "sequence_checksum", length = 20)
    private String sequenceChecksum;

    @Column(columnDefinition = "TEXT")
    private String sequence;

    // ── Evidence ──────────────────────────────────────────────────────────────
    @Column(name = "evidence_level", nullable = false)
    private Short evidenceLevel;

    // ── JSONB overflow ────────────────────────────────────────────────────────
    @Column(name = "metadata_jsonb", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadataJsonb;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Relationships ─────────────────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "protein_keyword",
        joinColumns = @JoinColumn(name = "protein_id"),
        inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    @Builder.Default
    private List<Keyword> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "proteinEntry", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProteinFeature> features = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "protein_go_term",
        joinColumns = @JoinColumn(name = "protein_id"),
        inverseJoinColumns = @JoinColumn(name = "go_term_id")
    )
    @Builder.Default
    private List<GoTerm> goTerms = new ArrayList<>();

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

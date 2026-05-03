package com.bioinformatics.dashboard.batch;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bioinformatics.dashboard.gene.entity.ProteinEntry;

@Configuration
public class ProteinEntryWriterConfig {

    @Bean
    public JdbcBatchItemWriter<ProteinEntry> proteinEntryItemWriter(DataSource dataSource) {

        String sql = """
                INSERT INTO protein_entry (
                    accession, entry_name, reviewed,
                    integrated_date, sequence_date, updated_date, sequence_version, entry_version,
                    protein_full_name, protein_short_name, protein_ec_number,
                    gene_name_primary, gene_name_synonyms, gene_orf_names, gene_ordered_locus,
                    organism_name, organism_common_name, taxid, lineage,
                    length, molecular_weight, sequence_checksum, sequence, evidence_level,
                    created_at, updated_at
                ) VALUES (
                    :accession, :entryName, :reviewed,
                    :integratedDate, :sequenceDate, :updatedDate, :sequenceVersion, :entryVersion,
                    :proteinFullName, :proteinShortName, :proteinEcNumber,
                    :geneNamePrimary, :geneNameSynonyms, :geneOrfNames, :geneOrderedLocus,
                    :organismName, :organismCommonName, :taxid, :lineage,
                    :length, :molecularWeight, :sequenceChecksum, :sequence, :evidenceLevel,
                    COALESCE(:createdAt, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP
                ) ON CONFLICT (accession) DO UPDATE SET
                    entry_name = EXCLUDED.entry_name,
                    reviewed = EXCLUDED.reviewed,
                    updated_date = EXCLUDED.updated_date,
                    sequence_version = EXCLUDED.sequence_version,
                    entry_version = EXCLUDED.entry_version,
                    protein_full_name = EXCLUDED.protein_full_name,
                    protein_short_name = EXCLUDED.protein_short_name,
                    protein_ec_number = EXCLUDED.protein_ec_number,
                    gene_name_primary = EXCLUDED.gene_name_primary,
                    gene_name_synonyms = EXCLUDED.gene_name_synonyms,
                    gene_orf_names = EXCLUDED.gene_orf_names,
                    gene_ordered_locus = EXCLUDED.gene_ordered_locus,
                    organism_name = EXCLUDED.organism_name,
                    organism_common_name = EXCLUDED.organism_common_name,
                    taxid = EXCLUDED.taxid,
                    lineage = EXCLUDED.lineage,
                    length = EXCLUDED.length,
                    molecular_weight = EXCLUDED.molecular_weight,
                    sequence_checksum = EXCLUDED.sequence_checksum,
                    sequence = EXCLUDED.sequence,
                    evidence_level = EXCLUDED.evidence_level,
                    updated_at = CURRENT_TIMESTAMP
                """;

        return new JdbcBatchItemWriterBuilder<ProteinEntry>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(sql)
                .dataSource(dataSource)
                .assertUpdates(false)
                .build();
    }
}
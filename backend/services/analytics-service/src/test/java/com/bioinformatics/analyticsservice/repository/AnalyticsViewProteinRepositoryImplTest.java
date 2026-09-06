package com.bioinformatics.analyticsservice.repository;

import com.bioinformatics.analyticsservice.providers.postgres.repository.AnalyticsProteinRepository;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AnalyticsViewProteinRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    @Qualifier("analyticsProteinRepository")
    private AnalyticsProteinRepository repository;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createQuery("DELETE FROM ProteinEntry").executeUpdate();
    }

    @Test
    void getDashboardKpis_shouldCalculateAggregationsCorrectly() {
        var reviewedProtein = createTestProtein("P12345", "PROT_A", true, 300, 30000, (short) 1);
        var unreviewedProtein = createTestProtein("P67890", "PROT_B", false, 500, 50000, (short) 4);

        entityManager.persist(reviewedProtein);
        entityManager.persist(unreviewedProtein);
        entityManager.flush();

        var kpis = repository.getDashboardKpis(null);

        assertThat(kpis).isNotNull();
        assertThat(kpis.totalProteins()).isEqualTo(2L);
        assertThat(kpis.reviewedCount()).isEqualTo(1L);
        assertThat(kpis.unreviewedCount()).isEqualTo(1L);
        assertThat(kpis.organismCount()).isEqualTo(1); // Both use "Homo sapiens"
        assertThat(kpis.avgLength()).isEqualTo(400L); // (300 + 500) / 2
        assertThat(kpis.minLength()).isEqualTo(300);
        assertThat(kpis.maxLength()).isEqualTo(500);
    }

    @Test
    void getDashboardKpis_shouldReturnZerosWhenEmpty() {
        var kpis = repository.getDashboardKpis(null);

        assertThat(kpis).isNotNull();
        assertThat(kpis.totalProteins()).isEqualTo(0L);
        assertThat(kpis.avgLength()).isEqualTo(0L);
    }

    @Test
    void getEvidenceLevels_shouldMapLevelsToLabels() {
        entityManager.persist(createTestProtein("P1", "E1", true, 100, 1000, (short) 1)); // Protein level
        entityManager.persist(createTestProtein("P2", "E2", true, 100, 1000, (short) 2)); // Transcript level
        entityManager.persist(createTestProtein("P3", "E3", true, 100, 1000, (short) 1)); // Protein level
        entityManager.flush();

        var evidenceLevels = repository.getEvidenceLevels(null);

        assertThat(evidenceLevels).hasSize(2);

        assertThat(evidenceLevels).anySatisfy(dto -> {
            assertThat(dto.evidenceLevel()).isEqualTo(1);
            assertThat(dto.label()).isEqualTo("Protein level");
            assertThat(dto.count()).isEqualTo(2L);
        });

        assertThat(evidenceLevels).anySatisfy(dto -> {
            assertThat(dto.evidenceLevel()).isEqualTo(2);
            assertThat(dto.label()).isEqualTo("Transcript level");
            assertThat(dto.count()).isEqualTo(1L);
        });
    }

    @Test
    void getAnalyticsSubset_shouldReturnEmptySubsetWhenNoData() {
        var subset = repository.getAnalyticsSubset(null);

        assertThat(subset).isNotNull();
        assertThat(subset.count()).isEqualTo(0L);
        assertThat(subset.reviewedRatio()).isEqualTo(0L);
        assertThat(subset.lengthDistribution()).isEmpty();
        assertThat(subset.evidenceDistribution()).isEmpty();
    }


    /**
     * Helper to create a valid ProteinEntry satisfying non-nullable constraints.
     */
    private ProteinEntry createTestProtein(String accession, String entryName, boolean reviewed,
                                           Integer length, Integer weight, Short evidenceLevel) {
        var protein = new ProteinEntry();
        protein.setAccession(accession);
        protein.setEntryName(entryName);
        protein.setReviewed(reviewed);
        protein.setOrganismName("Homo sapiens");
        protein.setTaxid(9606);
        protein.setLength(length);
        protein.setMolecularWeight(weight);
        protein.setEvidenceLevel(evidenceLevel);

        var now = Instant.now();
        protein.setCreatedAt(now);
        protein.setUpdatedAt(now);

        return protein;
    }
}
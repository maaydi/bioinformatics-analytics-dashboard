package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.AnalyticsSubsetDto;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class AnalyticsViewProteinRepositoryImpl implements AnalyticsViewProteinRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static <T> void injectSpecification(Specification<ProteinEntry> spec, Root<ProteinEntry> root, CriteriaQuery<T> query, CriteriaBuilder cb) {
        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }
    }

    @Override
    public DashboardKpisDto getDashboardKpis(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();

        var query = cb.createTupleQuery();
        var root = query.from(ProteinEntry.class);

        var totalProteinsExpr = cb.count(root);

        var reviewedCountExpr = cb.sum(
                cb.selectCase().when(cb.isTrue(root.get("reviewed")), 1L).otherwise(0L)
                        .as(Long.class)
        );

        var unreviewedCountExpr = cb.sum(
                cb.selectCase().when(cb.isFalse(root.get("reviewed")), 1L).otherwise(0L)
                        .as(Long.class)
        );

        var organismCountExpr = cb.countDistinct(root.get("organismName"));
        var taxonCountExpr = cb.countDistinct(root.get("taxid"));

        var avgLengthExpr = cb.function("round", Long.class, cb.avg(root.get("length")));
        var avgMolWeightExpr = cb.function("round", Long.class, cb.avg(root.get("molecularWeight")));

        var minLengthExpr = cb.min(root.get("length"));
        var maxLengthExpr = cb.max(root.get("length"));

        query.select(cb.tuple(
                        totalProteinsExpr,
                        reviewedCountExpr,
                        unreviewedCountExpr,
                        organismCountExpr,
                        taxonCountExpr,
                        avgLengthExpr,
                        avgMolWeightExpr,
                        minLengthExpr,
                        maxLengthExpr
                )

        );
        injectSpecification(spec, root, query, cb);
        var result = entityManager.createQuery(query).getSingleResult();

        long total = safeLong(result.get(0));
        if (total == 0) {
            return new DashboardKpisDto(0L, 0L, 0L, 0, 0, 0, 0L, 0, 0);
        }

        return new DashboardKpisDto(
                total,
                safeLong(result.get(1)),
                safeLong(result.get(2)),
                safeInt(result.get(3)),
                safeInt(result.get(4)),
                safeInt(result.get(5)),
                safeLong(result.get(6)),
                safeInt(result.get(7)),
                safeInt(result.get(8))
        );
    }

    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(LengthHistogramBucketDto.class);
        var root = query.from(ProteinEntry.class);

        var bucketExpr = cb.function(
                "width_bucket",
                Integer.class,
                root.get("length"),
                cb.literal(0),
                cb.literal(10000),
                cb.literal(100)
        );

        var countExpr = cb.count(root);

        query.select(cb.construct(LengthHistogramBucketDto.class, bucketExpr, countExpr));
        injectSpecification(spec, root, query, cb);
        query.groupBy(bucketExpr);
        query.orderBy(cb.asc(bucketExpr));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<OrganismCountDto> getByOrganism(int limit, Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(OrganismCountDto.class);
        var root = query.from(ProteinEntry.class);

        var organismNameExpr = root.get("organismName").as(String.class);
        var taxidExpr = root.get("taxid").as(Integer.class);

        var totalExpr = cb.count(root);

        var reviewedCountExpr = cb.sum(
                cb.selectCase()
                        .when(cb.isTrue(root.get("reviewed")), 1)
                        .otherwise(0)
                        .as(Integer.class)
        );

        var unreviewedCountExpr = cb.sum(
                cb.selectCase()
                        .when(cb.isFalse(root.get("reviewed")), 1)
                        .otherwise(0)
                        .as(Integer.class)
        );

        var avgLengthExpr = cb.function(
                "round",
                Integer.class,
                cb.avg(root.get("length"))
        );

        query.select(cb.construct(
                OrganismCountDto.class,
                organismNameExpr,
                taxidExpr,
                totalExpr,
                reviewedCountExpr,
                unreviewedCountExpr,
                avgLengthExpr
        ));
        injectSpecification(spec, root, query, cb);
        query.groupBy(organismNameExpr, taxidExpr);
        query.orderBy(cb.desc(totalExpr));

        return entityManager.createQuery(query).setMaxResults(limit).getResultList();

    }

    @Override
    public List<ReviewedRatioDto> getReviewedRatio(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(ReviewedRatioDto.class);
        var root = query.from(ProteinEntry.class);

        var reviewedExpr = root.get("reviewed");
        var countExpr = cb.count(root);

        query.select(cb.construct(
                ReviewedRatioDto.class,
                reviewedExpr,
                countExpr
        ));
        injectSpecification(spec, root, query, cb);
        query.groupBy(reviewedExpr);
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(EvidenceDistributionDto.class);
        var root = query.from(ProteinEntry.class);

        var evidenceLevelExpr = root.get("evidenceLevel").as(Integer.class);
        var countExpr = cb.count(root);

        var labelExpr = cb.selectCase(evidenceLevelExpr)
                .when(1, "Protein level")
                .when(2, "Transcript level")
                .when(3, "Homology")
                .when(4, "Predicted")
                .when(5, "Uncertain")
                .otherwise("Unknown")
                .as(String.class);

        query.select(cb.construct(
                EvidenceDistributionDto.class,
                evidenceLevelExpr,
                labelExpr,
                countExpr
        ));

        injectSpecification(spec, root, query, cb);

        query.groupBy(evidenceLevelExpr);
        query.orderBy(cb.asc(evidenceLevelExpr));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(KeywordFrequencyDto.class);

        var root = query.from(ProteinEntry.class);

        var keywordJoin = root.join("keywords");

        var keywordNameExpr = keywordJoin.get("name").as(String.class);
        var countExpr = cb.count(root);

        query.select(cb.construct(
                KeywordFrequencyDto.class,
                keywordNameExpr,
                countExpr
        ));
        injectSpecification(spec, root, query, cb);
        query.groupBy(keywordNameExpr);
        query.orderBy(cb.desc(countExpr));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Raw query to get count of proteins by length and molecular weight.
     * select length, molecular_weight, count(*) as protein_count
     * from protein_entry
     * group by length, molecular_weight
     *
     * @param spec Specification to filter proteins before grouping. Can be null for no filtering.
     * @return List of ProteinLengthWeightCount containing length, molecular weight and count of proteins for each combination.
     *
     */
    @Override
    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(ProteinLengthWeightCount.class);
        var root = query.from(ProteinEntry.class);

        var lengthExpr = root.get("length").as(Integer.class);
        var molecularWeightExpr = root.get("molecularWeight").as(Integer.class);

        var countExpr = cb.count(root);
        query.select(cb.construct(
                ProteinLengthWeightCount.class,
                lengthExpr,
                molecularWeightExpr,
                countExpr));

        injectSpecification(spec, root, query, cb);
        query.groupBy(lengthExpr, molecularWeightExpr);

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public AnalyticsSubsetDto getAnalyticsSubset(Specification<ProteinEntry> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var root = query.from(ProteinEntry.class);
        var countExpr = cb.count(root);
        var avgLengthExpr = cb.function("round", Long.class, cb.avg(root.get("length")));
        var reviewedCountExpr = cb.sum(
                cb.selectCase().when(cb.isTrue(root.get("reviewed")), 1L).otherwise(0L)
                        .as(Long.class)
        );
        query.select(cb.tuple(
                countExpr,
                avgLengthExpr,
                reviewedCountExpr
        ));
        injectSpecification(spec, root, query, cb);
        var result = entityManager.createQuery(query).getSingleResult();
        var count = safeLong(result.get(0));
        if (count == 0) {
            return new AnalyticsSubsetDto(0L, 0L, 0L, 0L, List.of(), List.of());
        }
        var avgLength = safeLong(result.get(1));
        var reviewedCount = safeLong(result.get(2));
        var reviewedRatio = reviewedCount * 100 / count;
        var lengthDist = getLengthHistogram(spec);
        var evidenceDist = getEvidenceLevels(spec);
        return new AnalyticsSubsetDto(
                count,
                avgLength,
                reviewedCount,
                reviewedRatio,
                lengthDist,
                evidenceDist
        );
    }


    private long safeLong(Object val) {
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    private int safeInt(Object val) {
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

}

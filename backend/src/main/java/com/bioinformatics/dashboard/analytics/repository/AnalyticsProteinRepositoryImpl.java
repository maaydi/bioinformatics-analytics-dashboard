package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class AnalyticsProteinRepositoryImpl implements AnalyticsProteinRepository {

    @PersistenceContext
    private EntityManager entityManager;

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }
        query.groupBy(organismNameExpr, taxidExpr);

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

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

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        query.groupBy(keywordNameExpr);
        query.orderBy(cb.desc(countExpr));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    private long safeLong(Object val) {
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    private int safeInt(Object val) {
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
}

package com.bioinformatics.common.gene.specification;

import com.bioinformatics.common.gene.entity.CrossReference;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class GeneSpecificationTest {
    @Mock
    private Root<ProteinEntry> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> path;
    @Mock
    private Expression<String> expression;
    @Mock
    private Expression<Boolean> booleanExpression;
    @Mock
    private Predicate predicate;
    @Mock
    private Join<Object, Object> join;

    @Test
    void fromRequest_NullRequest() {
        Specification<ProteinEntry> spec = GeneSpecification.fromRequest(null);
        assertNotNull(spec);
        Mockito.when(cb.conjunction()).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void fromRequest_EmptyRequest() {
        GeneSearchRequest request = new GeneSearchRequest(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );
        Specification<ProteinEntry> spec = GeneSpecification.fromRequest(request);
        assertNotNull(spec);
        Mockito.when(cb.conjunction()).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void globalSearch_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.globalSearch("test");
        assertNotNull(spec);
        Mockito.when(root.get("searchVector")).thenReturn((Path) path);
        Mockito.when(cb.literal("test")).thenReturn((Expression) expression);
        Mockito.when(cb.function(ArgumentMatchers.eq("fts_match"), ArgumentMatchers.eq(Boolean.class), ArgumentMatchers.eq(path), ArgumentMatchers.eq(expression))).thenReturn((Expression) booleanExpression);
        Mockito.when(cb.isTrue(booleanExpression)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void globalSearch_NullOrEmpty() {
        assertNull(GeneSpecification.globalSearch(null));
        assertNull(GeneSpecification.globalSearch("   "));
    }

    @Test
    void accession_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.accession("P12345");
        assertNotNull(spec);
        Mockito.when(root.get("accession")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, "P12345")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void accession_NullOrEmpty() {
        assertNull(GeneSpecification.accession(null));
        assertNull(GeneSpecification.accession(""));
    }

    @Test
    void entryName_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.entryName("Name");
        assertNotNull(spec);
        Mockito.when(root.get("entryName")).thenReturn((Path) path);
        Mockito.when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%name%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void entryName_NullOrEmpty() {
        assertNull(GeneSpecification.entryName(null));
    }

    @Test
    void geneNamePrimary_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.geneNamePrimary("Gene");
        assertNotNull(spec);
        Mockito.when(root.get("geneNamePrimary")).thenReturn((Path) path);
        Mockito.when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%gene%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void proteinFullName_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.proteinFullName("Protein");
        assertNotNull(spec);
        Mockito.when(root.get("proteinFullName")).thenReturn((Path) path);
        Mockito.when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%protein%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void reviewed_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.reviewed(true);
        assertNotNull(spec);
        Mockito.when(root.get("reviewed")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, true)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void organism_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.organism("Human");
        assertNotNull(spec);
        Mockito.when(root.get("organismName")).thenReturn((Path) path);
        Mockito.when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%human%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void taxid_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.taxid(9606);
        assertNotNull(spec);
        Mockito.when(root.get("taxid")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, 9606)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_BothBounds() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(100, 200);
        assertNotNull(spec);
        Mockito.when(root.get("length")).thenReturn((Path) path);
        Mockito.when(cb.between((Expression<Integer>) (Expression) path, 100, 200)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_MinOnly() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(100, null);
        assertNotNull(spec);
        Mockito.when(root.get("length")).thenReturn((Path) path);
        Mockito.when(cb.greaterThanOrEqualTo((Expression<Integer>) (Expression) path, 100)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_MaxOnly() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(null, 200);
        assertNotNull(spec);
        Mockito.when(root.get("length")).thenReturn((Path) path);
        Mockito.when(cb.lessThanOrEqualTo((Expression<Integer>) (Expression) path, 200)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void molecularWeightBetween_BothBounds() {
        Specification<ProteinEntry> spec = GeneSpecification.molecularWeightBetween(10, 50);
        assertNotNull(spec);
        Mockito.when(root.get("molecularWeight")).thenReturn((Path) path);
        Mockito.when(cb.between((Expression<Integer>) (Expression) path, 10, 50)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void evidenceLevels_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.evidenceLevels(List.of(1, 2));
        assertNotNull(spec);
        CriteriaBuilder.In inClause = Mockito.mock(CriteriaBuilder.In.class);
        Mockito.when(root.get("evidenceLevel")).thenReturn((Path) path);
        Mockito.when(path.in(List.of(1, 2))).thenReturn(inClause);
        assertEquals(inClause, spec.toPredicate(root, query, cb));
    }

    @Test
    void hasGoTermId_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.hasGoTermId("GO:0001");
        assertNotNull(spec);
        Mockito.when(root.join("goTerms", JoinType.INNER)).thenReturn(join);
        Mockito.when(join.get("goId")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, "GO:0001")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void keywords_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.keywords(List.of("Kinase", "ATP"));
        assertNotNull(spec);
        Mockito.when(root.join("keywords", JoinType.INNER)).thenReturn(join);
        Mockito.when(join.get("name")).thenReturn((Path) path);
        Mockito.when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%kinase%")).thenReturn(predicate);
        Mockito.when(cb.like(expression, "%atp%")).thenReturn(predicate);
        Mockito.when(cb.or(ArgumentMatchers.any(Predicate[].class))).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
        Mockito.verify(query).distinct(true);
    }

    @Test
    void keywords_EmptyList() {
        assertNull(GeneSpecification.keywords(Collections.emptyList()));
    }

    @Test
    void lineage_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.lineage("Eukaryota");
        assertNotNull(spec);
        Mockito.when(root.get("lineage")).thenReturn((Path) path);
        Mockito.when(cb.literal(",")).thenReturn((Expression) expression);
        Mockito.when(cb.function(ArgumentMatchers.eq("array_to_string"), ArgumentMatchers.eq(String.class), ArgumentMatchers.eq(path), ArgumentMatchers.eq(expression))).thenReturn(expression);
        Mockito.when(cb.lower((Expression<String>) (Expression) expression)).thenReturn(expression);
        Mockito.when(cb.like(expression, "%eukaryota%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void goAspect_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.goAspect("F");
        assertNotNull(spec);
        Mockito.when(root.join("goTerms", JoinType.INNER)).thenReturn(join);
        Mockito.when(join.get("aspect")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, 'F')).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void featureType_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.featureType("CHAIN");
        assertNotNull(spec);
        Mockito.when(root.join("features", JoinType.INNER)).thenReturn(join);
        Mockito.when(join.get("featureType")).thenReturn((Path) path);
        Mockito.when(cb.equal(path, "CHAIN")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void crossRefSource_ValidValue() {
        // 1. Arrange Mocks
        var testSource = "Pfam";
        Subquery<Integer> subquery = Mockito.mock(Subquery.class);
        Root<CrossReference> crossRefRoot = Mockito.mock(Root.class);
        Expression<Integer> literalExpression = Mockito.mock(Expression.class);

        Path<Object> proteinEntryPath = Mockito.mock(Path.class);
        Path<Object> sourcePath = Mockito.mock(Path.class);

        Predicate equalProteinEntryPredicate = Mockito.mock(Predicate.class);
        Predicate equalSourcePredicate = Mockito.mock(Predicate.class);
        Predicate existsPredicate = Mockito.mock(Predicate.class);

        Mockito.when(query.subquery(Integer.class)).thenReturn(subquery);
        Mockito.when(subquery.from(CrossReference.class)).thenReturn(crossRefRoot);
        Mockito.when(cb.literal(1)).thenReturn(literalExpression);
        Mockito.when(subquery.select(literalExpression)).thenReturn(subquery);
        Mockito.when(crossRefRoot.get("protein")).thenReturn(proteinEntryPath);
        Mockito.when(cb.equal(proteinEntryPath, root)).thenReturn(equalProteinEntryPredicate);
        Mockito.when(crossRefRoot.get("source")).thenReturn(sourcePath);
        Mockito.when(cb.equal(sourcePath, testSource)).thenReturn(equalSourcePredicate);
        Mockito.when(subquery.where(equalProteinEntryPredicate, equalSourcePredicate)).thenReturn(subquery);
        Mockito.when(cb.exists(subquery)).thenReturn(existsPredicate);

        var spec = GeneSpecification.crossRefSource(testSource);
        assertNotNull(spec);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(existsPredicate, result);

        Mockito.verify(query).subquery(Integer.class);
        Mockito.verify(subquery).from(CrossReference.class);
        Mockito.verify(subquery).select(literalExpression);
        Mockito.verify(subquery).where(equalProteinEntryPredicate, equalSourcePredicate);
        Mockito.verify(cb).exists(subquery);
    }
}

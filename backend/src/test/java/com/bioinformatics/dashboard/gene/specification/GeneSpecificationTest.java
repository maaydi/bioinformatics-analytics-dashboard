package com.bioinformatics.dashboard.gene.specification;

import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.providers.postgres.gene.specification.GeneSpecification;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        when(cb.conjunction()).thenReturn(predicate);
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
        when(cb.conjunction()).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void globalSearch_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.globalSearch("test");
        assertNotNull(spec);
        when(root.get("searchVector")).thenReturn((Path) path);
        when(cb.literal("test")).thenReturn((Expression) expression);
        when(cb.function(eq("fts_match"), eq(Boolean.class), eq(path), eq(expression))).thenReturn((Expression) booleanExpression);
        when(cb.isTrue(booleanExpression)).thenReturn(predicate);
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
        when(root.get("accession")).thenReturn((Path) path);
        when(cb.equal(path, "P12345")).thenReturn(predicate);
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
        when(root.get("entryName")).thenReturn((Path) path);
        when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        when(cb.like(expression, "%name%")).thenReturn(predicate);
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
        when(root.get("geneNamePrimary")).thenReturn((Path) path);
        when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        when(cb.like(expression, "%gene%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void proteinFullName_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.proteinFullName("Protein");
        assertNotNull(spec);
        when(root.get("proteinFullName")).thenReturn((Path) path);
        when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        when(cb.like(expression, "%protein%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void reviewed_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.reviewed(true);
        assertNotNull(spec);
        when(root.get("reviewed")).thenReturn((Path) path);
        when(cb.equal(path, true)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void organism_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.organism("Human");
        assertNotNull(spec);
        when(root.get("organismName")).thenReturn((Path) path);
        when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        when(cb.like(expression, "%human%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void taxid_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.taxid(9606);
        assertNotNull(spec);
        when(root.get("taxid")).thenReturn((Path) path);
        when(cb.equal(path, 9606)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_BothBounds() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(100, 200);
        assertNotNull(spec);
        when(root.get("length")).thenReturn((Path) path);
        when(cb.between((Expression<Integer>) (Expression) path, 100, 200)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_MinOnly() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(100, null);
        assertNotNull(spec);
        when(root.get("length")).thenReturn((Path) path);
        when(cb.greaterThanOrEqualTo((Expression<Integer>) (Expression) path, 100)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void lengthBetween_MaxOnly() {
        Specification<ProteinEntry> spec = GeneSpecification.lengthBetween(null, 200);
        assertNotNull(spec);
        when(root.get("length")).thenReturn((Path) path);
        when(cb.lessThanOrEqualTo((Expression<Integer>) (Expression) path, 200)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void molecularWeightBetween_BothBounds() {
        Specification<ProteinEntry> spec = GeneSpecification.molecularWeightBetween(10, 50);
        assertNotNull(spec);
        when(root.get("molecularWeight")).thenReturn((Path) path);
        when(cb.between((Expression<Integer>) (Expression) path, 10, 50)).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void evidenceLevels_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.evidenceLevels(List.of(1, 2));
        assertNotNull(spec);
        CriteriaBuilder.In inClause = mock(CriteriaBuilder.In.class);
        when(root.get("evidenceLevel")).thenReturn((Path) path);
        when(path.in(List.of(1, 2))).thenReturn(inClause);
        assertEquals(inClause, spec.toPredicate(root, query, cb));
    }

    @Test
    void hasGoTermId_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.hasGoTermId("GO:0001");
        assertNotNull(spec);
        when(root.join("goTerms", JoinType.INNER)).thenReturn(join);
        when(join.get("goId")).thenReturn((Path) path);
        when(cb.equal(path, "GO:0001")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void keywords_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.keywords(List.of("Kinase", "ATP"));
        assertNotNull(spec);
        when(root.join("keywords", JoinType.INNER)).thenReturn(join);
        when(join.get("name")).thenReturn((Path) path);
        when(cb.lower((Expression<String>) (Expression) path)).thenReturn(expression);
        when(cb.like(expression, "%kinase%")).thenReturn(predicate);
        when(cb.like(expression, "%atp%")).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
        verify(query).distinct(true);
    }

    @Test
    void keywords_EmptyList() {
        assertNull(GeneSpecification.keywords(Collections.emptyList()));
    }

    @Test
    void lineage_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.lineage("Eukaryota");
        assertNotNull(spec);
        when(root.get("lineage")).thenReturn((Path) path);
        when(cb.literal(",")).thenReturn((Expression) expression);
        when(cb.function(eq("array_to_string"), eq(String.class), eq(path), eq(expression))).thenReturn(expression);
        when(cb.lower((Expression<String>) (Expression) expression)).thenReturn(expression);
        when(cb.like(expression, "%eukaryota%")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void goAspect_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.goAspect("F");
        assertNotNull(spec);
        when(root.join("goTerms", JoinType.INNER)).thenReturn(join);
        when(join.get("aspect")).thenReturn((Path) path);
        when(cb.equal(path, 'F')).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void featureType_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.featureType("CHAIN");
        assertNotNull(spec);
        when(root.join("features", JoinType.INNER)).thenReturn(join);
        when(join.get("featureType")).thenReturn((Path) path);
        when(cb.equal(path, "CHAIN")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
    }

    @Test
    void crossRefSource_ValidValue() {
        Specification<ProteinEntry> spec = GeneSpecification.crossRefSource("Pfam");
        assertNotNull(spec);
        when(root.join("crossReferences")).thenReturn(join);
        when(join.get("source")).thenReturn((Path) path);
        when(cb.equal(path, "Pfam")).thenReturn(predicate);
        assertEquals(predicate, spec.toPredicate(root, query, cb));
        verify(query).distinct(true);
    }
}

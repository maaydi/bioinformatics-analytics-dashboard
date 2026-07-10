package com.bioinformatics.dashboard.providers.uniprotkb.gene.specification;

import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneSpecification")
class GeneSpecificationTest {

    // ---------------------------------------------------------------------------
    // Helper: build a request with every field null (no pagination fields needed)
    // ---------------------------------------------------------------------------
    private static GeneSearchRequest emptyRequest() {
        return new GeneSearchRequest(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null
        );
    }

    private static GeneSearchRequest requestWith(
            String globalSearch,
            String accession,
            String entryName,
            String geneNamePrimary,
            String proteinFullName,
            Boolean reviewed,
            String organism,
            Integer taxid,
            String lineage,
            Integer lengthMin,
            Integer lengthMax,
            Integer molecularWeightMin,
            Integer molecularWeightMax,
            List<Integer> evidenceLevels,
            List<String> keywords,
            String goTermId,
            String goAspect,
            String featureType,
            String crossRefSource
    ) {
        return new GeneSearchRequest(
                globalSearch, accession, entryName, geneNamePrimary, proteinFullName,
                reviewed, organism, taxid, lineage, lengthMin, lengthMax,
                molecularWeightMin, molecularWeightMax, evidenceLevels, keywords,
                goTermId, goAspect, featureType, crossRefSource,
                null, null, null, null
        );
    }

    // ==========================================================================
    // fromRequest
    // ==========================================================================
    @Nested
    @DisplayName("fromRequest(GeneSearchRequest)")
    class FromRequest {

        @Test
        @DisplayName("null request → wildcard query (*)")
        void nullRequest_returnsWildcard() {
            var result = GeneSpecification.fromRequest(null);

            assertThat(result).isEqualTo("(*)");
        }

        @Test
        @DisplayName("all-null fields → wildcard query (*)")
        void allNullFields_returnsWildcard() {
            var result = GeneSpecification.fromRequest(emptyRequest());

            assertThat(result).isEqualTo("(*)");
        }

        @Test
        @DisplayName("single active field (accession) → only that clause")
        void singleActiveField_accession_producesCorrectClause() {
            var req = requestWith(
                    null, "P12345", null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null
            );

            var result = GeneSpecification.fromRequest(req);

            assertThat(result).isEqualTo("accession:P12345");
        }

        @Test
        @DisplayName("single active field (reviewed=true) → only that clause")
        void singleActiveField_reviewed_producesCorrectClause() {
            var req = requestWith(
                    null, null, null, null, null,
                    true, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null
            );

            var result = GeneSpecification.fromRequest(req);

            assertThat(result).isEqualTo("reviewed:true");
        }

        @Test
        @DisplayName("multiple active fields → clauses joined with ' AND '")
        void multipleActiveFields_joinsWithAnd() {
            var req = requestWith(
                    null, "P12345", null, "BRCA1", null,
                    true, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null
            );

            var result = GeneSpecification.fromRequest(req);

            assertThat(result).isEqualTo("accession:P12345 AND gene:BRCA1* AND reviewed:true");
        }

        @Test
        @DisplayName("all searchable fields set → all clauses joined with ' AND '")
        void allFieldsSet_producesFullQueryWithAnd() {
            var req = requestWith(
                    "kinase", "P12345", "BRCA1_HUMAN", "BRCA1", "Breast cancer type 1",
                    true, "Homo sapiens", 9606, "Mammalia", 100, 500,
                    10000, 60000, List.of(1, 2), List.of("Kinase"),
                    "GO:0006915", null, "Signal peptide", "PDB"
            );

            var result = GeneSpecification.fromRequest(req);

            assertThat(result)
                    .contains("(kinase*)")
                    .contains("accession:P12345")
                    .contains("id:BRCA1_HUMAN*")
                    .contains("gene:BRCA1*")
                    .contains("protein_name:Breast cancer type 1*")
                    .contains("reviewed:true")
                    .contains("organism_name:Homo sapiens*")
                    .contains("organism_id:9606")
                    .contains("taxonomy_name:Mammalia*")
                    .contains("length:[100 TO 500]")
                    .contains("mass:[10000 TO 60000]")
                    .contains("(existence:1 OR existence:2)")
                    .contains("(keyword:Kinase*)")
                    .contains("go:0006915")
                    .contains("ft_signal_peptide:*")
                    .contains("database:pdb");

            // Every clause must be separated by AND
            // 16 active fields: globalSearch, accession, entryName, geneNamePrimary,
            // proteinFullName, reviewed, organism, taxid, lineage, lengthBetween,
            // molecularWeightBetween, evidenceLevels, keywords, goTermId, featureType, crossRefSource
            var clauses = result.split(" AND ");
            assertThat(clauses).hasSize(16);
        }
    }

    // ==========================================================================
    // globalSearch
    // ==========================================================================
    @Nested
    @DisplayName("globalSearch(String)")
    class GlobalSearch {

        @Test
        @DisplayName("null input → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.globalSearch(null)).isEmpty();
        }

        @Test
        @DisplayName("blank input → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.globalSearch("   ")).isEmpty();
        }

        @Test
        @DisplayName("empty string → Optional.empty()")
        void emptyString_returnsEmpty() {
            assertThat(GeneSpecification.globalSearch("")).isEmpty();
        }

        @Test
        @DisplayName("plain query → wraps in parentheses with wildcard")
        void plainQuery_wrapsWithWildcard() {
            var result = GeneSpecification.globalSearch("kinase");

            assertThat(result).contains("(kinase*)");
        }

        @Test
        @DisplayName("query with leading/trailing spaces → trimmed before wrapping")
        void queryWithSpaces_isTrimmed() {
            var result = GeneSpecification.globalSearch("  kinase  ");

            assertThat(result).contains("(kinase*)");
        }

        @Test
        @DisplayName("query with '+' → '+' is escaped")
        void queryWithPlus_escapesPlus() {
            var result = GeneSpecification.globalSearch("p53+kinase");

            assertThat(result).hasValue("(p53\\+kinase*)");
        }

        @Test
        @DisplayName("query with '-' → '-' is escaped")
        void queryWithDash_escapesDash() {
            var result = GeneSpecification.globalSearch("alpha-1");

            assertThat(result).hasValue("(alpha\\-1*)");
        }

        @Test
        @DisplayName("query with parentheses → parentheses are escaped")
        void queryWithParentheses_escapesParentheses() {
            var result = GeneSpecification.globalSearch("signal (peptide)");

            assertThat(result).hasValue("(signal \\(peptide\\)*)");
        }

        @Test
        @DisplayName("query with colon → colon is escaped")
        void queryWithColon_escapesColon() {
            var result = GeneSpecification.globalSearch("GO:001");

            assertThat(result).hasValue("(GO\\:001*)");
        }

        @Test
        @DisplayName("query with '&&' → '&&' is escaped")
        void queryWithDoubleAmpersand_escapesDoubleAmpersand() {
            var result = GeneSpecification.globalSearch("alpha&&beta");

            assertThat(result).hasValue("(alpha\\&&beta*)");
        }

        @Test
        @DisplayName("query with '||' → '||' is escaped")
        void queryWithDoublePipe_escapesDoublePipe() {
            var result = GeneSpecification.globalSearch("alpha||beta");

            assertThat(result).hasValue("(alpha\\||beta*)");
        }

        @Test
        @DisplayName("query with multiple special chars → all special chars escaped")
        void queryWithMultipleSpecialChars_escapesAll() {
            var result = GeneSpecification.globalSearch("p53[active]");

            assertThat(result).hasValue("(p53\\[active\\]*)");
        }
    }

    // ==========================================================================
    // accession
    // ==========================================================================
    @Nested
    @DisplayName("accession(String)")
    class Accession {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.accession(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.accession("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid accession → 'accession:VALUE' in uppercase")
        void validAccession_returnsUppercasedClause() {
            var result = GeneSpecification.accession("p12345");

            assertThat(result).hasValue("accession:P12345");
        }

        @Test
        @DisplayName("already uppercase accession → unchanged")
        void uppercaseAccession_isUnchanged() {
            var result = GeneSpecification.accession("P12345");

            assertThat(result).hasValue("accession:P12345");
        }

        @Test
        @DisplayName("accession with surrounding whitespace → trimmed and uppercased")
        void accessionWithSpaces_isTrimmedAndUppercased() {
            var result = GeneSpecification.accession("  p12345  ");

            assertThat(result).hasValue("accession:P12345");
        }
    }

    // ==========================================================================
    // entryName
    // ==========================================================================
    @Nested
    @DisplayName("entryName(String)")
    class EntryName {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.entryName(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.entryName("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid entry name → 'id:VALUE*' in uppercase with wildcard")
        void validEntryName_returnsUppercasedClauseWithWildcard() {
            var result = GeneSpecification.entryName("brca1_human");

            assertThat(result).hasValue("id:BRCA1_HUMAN*");
        }

        @Test
        @DisplayName("entry name with whitespace → trimmed before uppercasing")
        void entryNameWithSpaces_isTrimmed() {
            var result = GeneSpecification.entryName("  BRCA1_HUMAN  ");

            assertThat(result).hasValue("id:BRCA1_HUMAN*");
        }
    }

    // ==========================================================================
    // geneNamePrimary
    // ==========================================================================
    @Nested
    @DisplayName("geneNamePrimary(String)")
    class GeneNamePrimary {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.geneNamePrimary(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.geneNamePrimary("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid gene name → 'gene:VALUE*'")
        void validGeneName_returnsClauseWithWildcard() {
            var result = GeneSpecification.geneNamePrimary("BRCA1");

            assertThat(result).hasValue("gene:BRCA1*");
        }

        @Test
        @DisplayName("gene name preserves original casing")
        void geneName_preservesCasing() {
            var result = GeneSpecification.geneNamePrimary("brca1");

            assertThat(result).hasValue("gene:brca1*");
        }
    }

    // ==========================================================================
    // proteinFullName
    // ==========================================================================
    @Nested
    @DisplayName("proteinFullName(String)")
    class ProteinFullName {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.proteinFullName(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.proteinFullName("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid protein name → 'protein_name:VALUE*'")
        void validProteinName_returnsClauseWithWildcard() {
            var result = GeneSpecification.proteinFullName("Breast cancer type 1");

            assertThat(result).hasValue("protein_name:Breast cancer type 1*");
        }
    }

    // ==========================================================================
    // reviewed
    // ==========================================================================
    @Nested
    @DisplayName("reviewed(Boolean)")
    class Reviewed {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.reviewed(null)).isEmpty();
        }

        @Test
        @DisplayName("true → 'reviewed:true'")
        void true_returnsReviewedTrue() {
            assertThat(GeneSpecification.reviewed(true)).hasValue("reviewed:true");
        }

        @Test
        @DisplayName("false → 'reviewed:false'")
        void false_returnsReviewedFalse() {
            assertThat(GeneSpecification.reviewed(false)).hasValue("reviewed:false");
        }
    }

    // ==========================================================================
    // organism
    // ==========================================================================
    @Nested
    @DisplayName("organism(String)")
    class Organism {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.organism(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.organism("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid organism → 'organism_name:VALUE*'")
        void validOrganism_returnsClauseWithWildcard() {
            var result = GeneSpecification.organism("Homo sapiens");

            assertThat(result).hasValue("organism_name:Homo sapiens*");
        }

        @Test
        @DisplayName("organism with surrounding whitespace → trimmed")
        void organismWithSpaces_isTrimmed() {
            var result = GeneSpecification.organism("  Homo sapiens  ");

            assertThat(result).hasValue("organism_name:Homo sapiens*");
        }
    }

    // ==========================================================================
    // taxid
    // ==========================================================================
    @Nested
    @DisplayName("taxid(Integer)")
    class Taxid {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.taxid(null)).isEmpty();
        }

        @Test
        @DisplayName("valid taxid → 'organism_id:VALUE'")
        void validTaxid_returnsCorrectClause() {
            assertThat(GeneSpecification.taxid(9606)).hasValue("organism_id:9606");
        }
    }

    // ==========================================================================
    // lineage
    // ==========================================================================
    @Nested
    @DisplayName("lineage(String)")
    class Lineage {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.lineage(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.lineage("  ")).isEmpty();
        }

        @Test
        @DisplayName("valid lineage → 'taxonomy_name:VALUE*'")
        void validLineage_returnsClauseWithWildcard() {
            var result = GeneSpecification.lineage("Mammalia");

            assertThat(result).hasValue("taxonomy_name:Mammalia*");
        }
    }

    // ==========================================================================
    // lengthBetween
    // ==========================================================================
    @Nested
    @DisplayName("lengthBetween(Integer, Integer)")
    class LengthBetween {

        @Test
        @DisplayName("both null → Optional.empty()")
        void bothNull_returnsEmpty() {
            assertThat(GeneSpecification.lengthBetween(null, null)).isEmpty();
        }

        @Test
        @DisplayName("min only → 'length:[min TO *]'")
        void minOnly_usesWildcardForUpperBound() {
            var result = GeneSpecification.lengthBetween(100, null);

            assertThat(result).hasValue("length:[100 TO *]");
        }

        @Test
        @DisplayName("max only → 'length:[* TO max]'")
        void maxOnly_usesWildcardForLowerBound() {
            var result = GeneSpecification.lengthBetween(null, 500);

            assertThat(result).hasValue("length:[* TO 500]");
        }

        @Test
        @DisplayName("both bounds provided → 'length:[min TO max]'")
        void bothBounds_producesClosedRange() {
            var result = GeneSpecification.lengthBetween(100, 500);

            assertThat(result).hasValue("length:[100 TO 500]");
        }
    }

    // ==========================================================================
    // molecularWeightBetween
    // ==========================================================================
    @Nested
    @DisplayName("molecularWeightBetween(Integer, Integer)")
    class MolecularWeightBetween {

        @Test
        @DisplayName("both null → Optional.empty()")
        void bothNull_returnsEmpty() {
            assertThat(GeneSpecification.molecularWeightBetween(null, null)).isEmpty();
        }

        @Test
        @DisplayName("min only → 'mass:[min TO *]'")
        void minOnly_usesWildcardForUpperBound() {
            var result = GeneSpecification.molecularWeightBetween(1000, null);

            assertThat(result).hasValue("mass:[1000 TO *]");
        }

        @Test
        @DisplayName("max only → 'mass:[* TO max]'")
        void maxOnly_usesWildcardForLowerBound() {
            var result = GeneSpecification.molecularWeightBetween(null, 50000);

            assertThat(result).hasValue("mass:[* TO 50000]");
        }

        @Test
        @DisplayName("both bounds provided → 'mass:[min TO max]'")
        void bothBounds_producesClosedRange() {
            var result = GeneSpecification.molecularWeightBetween(10000, 60000);

            assertThat(result).hasValue("mass:[10000 TO 60000]");
        }
    }

    // ==========================================================================
    // evidenceLevels
    // ==========================================================================
    @Nested
    @DisplayName("evidenceLevels(List<Integer>)")
    class EvidenceLevels {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.evidenceLevels(null)).isEmpty();
        }

        @Test
        @DisplayName("empty list → Optional.empty()")
        void emptyList_returnsEmpty() {
            assertThat(GeneSpecification.evidenceLevels(List.of())).isEmpty();
        }

        @Test
        @DisplayName("single level → '(existence:N)'")
        void singleLevel_returnsSingleExistenceClause() {
            var result = GeneSpecification.evidenceLevels(List.of(1));

            assertThat(result).hasValue("(existence:1)");
        }

        @Test
        @DisplayName("multiple levels → levels joined with ' OR ' inside parentheses")
        void multipleLevels_joinsWithOr() {
            var result = GeneSpecification.evidenceLevels(List.of(1, 2, 3));

            assertThat(result).hasValue("(existence:1 OR existence:2 OR existence:3)");
        }
    }

    // ==========================================================================
    // keywords
    // ==========================================================================
    @Nested
    @DisplayName("keywords(List<String>)")
    class Keywords {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.keywords(null)).isEmpty();
        }

        @Test
        @DisplayName("empty list → Optional.empty()")
        void emptyList_returnsEmpty() {
            assertThat(GeneSpecification.keywords(List.of())).isEmpty();
        }

        @Test
        @DisplayName("list containing only blank entries → Optional.empty()")
        void allBlankEntries_returnsEmpty() {
            assertThat(GeneSpecification.keywords(List.of("", "  ", "\t"))).isEmpty();
        }

        @Test
        @DisplayName("single keyword → '(keyword:TERM*)'")
        void singleKeyword_returnsCorrectClause() {
            var result = GeneSpecification.keywords(List.of("Kinase"));

            assertThat(result).hasValue("(keyword:Kinase*)");
        }

        @Test
        @DisplayName("multiple keywords → keywords joined with ' OR ' inside parentheses")
        void multipleKeywords_joinsWithOr() {
            var result = GeneSpecification.keywords(List.of("Kinase", "Phosphoprotein"));

            assertThat(result).hasValue("(keyword:Kinase* OR keyword:Phosphoprotein*)");
        }

        @Test
        @DisplayName("list with blank and valid entries → blank entries filtered out")
        void mixedList_filtersBlankEntries() {
            var result = GeneSpecification.keywords(List.of("Kinase", "  ", "Phosphoprotein"));

            assertThat(result).hasValue("(keyword:Kinase* OR keyword:Phosphoprotein*)");
        }

        @Test
        @DisplayName("keyword with surrounding whitespace → trimmed in the clause")
        void keywordWithWhitespace_isTrimmed() {
            var result = GeneSpecification.keywords(List.of("  Kinase  "));

            assertThat(result).hasValue("(keyword:Kinase*)");
        }
    }

    // ==========================================================================
    // goTermId
    // ==========================================================================
    @Nested
    @DisplayName("goTermId(String)")
    class GoTermId {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.goTermId(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.goTermId("  ")).isEmpty();
        }

        @Test
        @DisplayName("input with 'GO:' prefix → strips prefix and returns 'go:NUMERIC_ID'")
        void withGoPrefix_stripsPrefix() {
            var result = GeneSpecification.goTermId("GO:0006915");

            assertThat(result).hasValue("go:0006915");
        }

        @Test
        @DisplayName("input without 'GO:' prefix → returns 'go:INPUT' as-is")
        void withoutGoPrefix_usesInputDirectly() {
            var result = GeneSpecification.goTermId("0006915");

            assertThat(result).hasValue("go:0006915");
        }
    }

    // ==========================================================================
    // featureType
    // ==========================================================================
    @Nested
    @DisplayName("featureType(String)")
    class FeatureType {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.featureType(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.featureType("  ")).isEmpty();
        }

        @Test
        @DisplayName("uppercase input → lowercased in 'ft_TYPE:*'")
        void uppercaseInput_isLowercased() {
            var result = GeneSpecification.featureType("SIGNAL");

            assertThat(result).hasValue("ft_signal:*");
        }

        @Test
        @DisplayName("space-separated input → spaces replaced by underscores")
        void spaceSeparatedInput_replacesSpacesWithUnderscores() {
            var result = GeneSpecification.featureType("Signal Peptide");

            assertThat(result).hasValue("ft_signal_peptide:*");
        }

        @Test
        @DisplayName("already lowercase single word → 'ft_TYPE:*'")
        void lowercaseSingleWord_returnsCorrectClause() {
            var result = GeneSpecification.featureType("helix");

            assertThat(result).hasValue("ft_helix:*");
        }
    }

    // ==========================================================================
    // crossRefSource
    // ==========================================================================
    @Nested
    @DisplayName("crossRefSource(String)")
    class CrossRefSource {

        @Test
        @DisplayName("null → Optional.empty()")
        void null_returnsEmpty() {
            assertThat(GeneSpecification.crossRefSource(null)).isEmpty();
        }

        @Test
        @DisplayName("blank → Optional.empty()")
        void blank_returnsEmpty() {
            assertThat(GeneSpecification.crossRefSource("  ")).isEmpty();
        }

        @Test
        @DisplayName("uppercase source → lowercased in 'database:SOURCE'")
        void uppercaseSource_isLowercased() {
            var result = GeneSpecification.crossRefSource("PDB");

            assertThat(result).hasValue("database:pdb");
        }

        @Test
        @DisplayName("source with surrounding whitespace → trimmed and lowercased")
        void sourceWithSpaces_isTrimmedAndLowercased() {
            var result = GeneSpecification.crossRefSource("  PDB  ");

            assertThat(result).hasValue("database:pdb");
        }

        @Test
        @DisplayName("already lowercase source → unchanged")
        void lowercaseSource_isUnchanged() {
            var result = GeneSpecification.crossRefSource("pdb");

            assertThat(result).hasValue("database:pdb");
        }
    }
}


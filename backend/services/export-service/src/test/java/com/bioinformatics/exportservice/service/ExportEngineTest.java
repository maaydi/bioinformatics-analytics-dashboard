package com.bioinformatics.exportservice.service;

import com.bioinformatics.shared.models.gene.ExportFieldSchema;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportEngineTest {

    @Test
    void getAvailableFieldsForExport_returnsAnnotatedRecordComponents() {
        var fields = ExportEngine.getAvailableFieldsForExport(TestExportRecord.class);

        assertThat(fields)
                .extracting(ExportFieldSchema::fieldName)
                .containsExactly("accession", "custom_name", "customDefault");

        assertThat(fields)
                .first()
                .satisfies(field -> {
                    assertThat(field.displayName()).isEqualTo("Accession");
                    assertThat(field.dataType()).isEqualTo("String");
                    assertThat(field.description()).isEqualTo("UniProt accession");
                    assertThat(field.available()).isTrue();
                });

        assertThat(fields)
                .element(1)
                .satisfies(field -> {
                    assertThat(field.displayName()).isEqualTo("Custom label");
                    assertThat(field.dataType()).isEqualTo("Long");
                    assertThat(field.available()).isFalse();
                });
    }

    @Test
    void getExcludedFields_returnsIgnoredComponentsOnly() {
        var excluded = ExportEngine.getExcludedFields(TestExportRecord.class);

        assertThat(excluded).containsExactly("internalId", "auditStamp");
    }

    @Test
    void getAvailableFieldsForExport_returnsEmptyListForNonRecord() {
        var fields = ExportEngine.getAvailableFieldsForExport(PlainClass.class);

        assertThat(fields).isEmpty();
    }

    private record TestExportRecord(
            @ExportField(name = "accession", displayName = "Accession", description = "UniProt accession")
            String accession,

            @ExportField(name = "custom_name", displayName = "Custom label", dataType = Long.class, description = "Custom field", available = false)
            Long customName,

            @ExportField(description = "Uses default attribute names")
            String customDefault,

            @ExportIgnore
            Long internalId,

            @ExportIgnore
            String auditStamp
    ) {
    }

    private static class PlainClass {
        private String ignored;
    }
}

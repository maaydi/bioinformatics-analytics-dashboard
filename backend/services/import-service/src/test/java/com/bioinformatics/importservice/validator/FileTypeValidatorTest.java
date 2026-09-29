package com.bioinformatics.importservice.validator;

import com.bioinformatics.common.exception.UnsupportedFileTypeException;
import com.bioinformatics.importservice.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypeValidatorTest {

    @Test
    void isValidReturnsTrueForSupportedExtension() {
        var validator = new FileTypeValidator(new ApplicationProperties(
                new ApplicationProperties.ImportConfig("/tmp/import", List.of("dat", "tsv"), null),
                null
        ));

        var file = new MockMultipartFile("file", "sample.tsv", "text/plain", "data".getBytes());

        assertThat(validator.isValid(file, null)).isTrue();
    }

    @Test
    void isValidThrowsForUnsupportedExtension() {
        var validator = new FileTypeValidator(new ApplicationProperties(
                new ApplicationProperties.ImportConfig("/tmp/import", List.of("dat", "tsv"), null),
                null
        ));

        var file = new MockMultipartFile("file", "sample.fasta", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> validator.isValid(file, null))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("Extension fasta is not supported.");
    }

    @Test
    void isValidRejectsNullOrEmptyFiles() {
        var validator = new FileTypeValidator(new ApplicationProperties(
                new ApplicationProperties.ImportConfig("/tmp/import", List.of("dat", "tsv"), null),
                null
        ));

        assertThat(validator.isValid(null, null)).isFalse();
        assertThat(validator.isValid(new MockMultipartFile("file", "sample.tsv", "text/plain", new byte[0]), null)).isFalse();
    }
}

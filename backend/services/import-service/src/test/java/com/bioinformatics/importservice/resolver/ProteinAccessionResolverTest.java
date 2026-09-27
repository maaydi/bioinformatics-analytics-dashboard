package com.bioinformatics.importservice.resolver;

import com.bioinformatics.common.gene.service.ProteinEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProteinAccessionResolverTest {

    @Mock
    private ProteinEntryService proteinEntryService;

    private ProteinAccessionResolver resolver;

    @BeforeEach
    void setUp() {
        when(proteinEntryService.findAllAccessions()).thenReturn(List.of("P12345", "P67890"));
        resolver = new ProteinAccessionResolver(proteinEntryService);
        resolver.init();
    }

    @Test
    void alreadyExistsReturnsTrueForDuplicateAccession() {
        assertThat(resolver.alreadyExists("P12345")).isTrue();
    }

    @Test
    void alreadyExistsReturnsFalseForNewAccessionAndTracksIt() {
        assertThat(resolver.alreadyExists("P99999")).isFalse();
        assertThat(resolver.alreadyExists("P99999")).isTrue();
    }
}


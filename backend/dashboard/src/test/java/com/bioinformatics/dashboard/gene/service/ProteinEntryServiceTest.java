package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.common.gene.entity.CrossReference;
import com.bioinformatics.common.gene.entity.ProteinComment;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinCommentRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinPublicationRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.service.ProteinEntryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProteinEntryServiceTest {
    @Mock
    private ProteinEntryRepository proteinEntryRepository;
    @Mock
    private CrossReferenceRepository crossReferenceRepository;
    @Mock
    private ProteinCommentRepository proteinCommentRepository;
    @Mock
    private ProteinPublicationRepository proteinPublicationRepository;
    @InjectMocks
    private ProteinEntryService proteinEntryService;

    @Test
    @DisplayName("Should find protein by accession")
    void shouldFindByAccession() {
        // Arrange
        String accession = "P12345";
        ProteinEntry entry = new ProteinEntry();
        entry.setAccession(accession);
        when(proteinEntryRepository.findByAccession(accession)).thenReturn(Optional.of(entry));
        // Act
        Optional<ProteinEntry> result = proteinEntryService.findByAccession(accession);
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAccession()).isEqualTo(accession);
        verify(proteinEntryRepository).findByAccession(accession);
    }

    @Test
    @DisplayName("Should check if accession exists")
    void shouldExistByAccession() {
        // Arrange
        String accession = "P12345";
        when(proteinEntryRepository.existsByAccession(accession)).thenReturn(true);
        // Act
        boolean exists = proteinEntryService.existsByAccession(accession);
        // Assert
        assertThat(exists).isTrue();
        verify(proteinEntryRepository).existsByAccession(accession);
    }

    @Test
    @DisplayName("Should find base details by ID")
    void shouldFindBaseDetails() {
        // Arrange
        var accession = "ACC";
        ProteinEntry entry = new ProteinEntry();
        entry.setAccession(accession);
        when(proteinEntryRepository.findBaseDetails(accession)).thenReturn(Optional.of(entry));
        // Act
        Optional<ProteinEntry> result = proteinEntryService.findBaseDetails(accession);
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAccession()).isEqualTo(accession);
        verify(proteinEntryRepository).findBaseDetails(accession);
    }

    @Test
    @DisplayName("Should find additional details and populate transient collections")
    void shouldFindAdditionalDetails() {
        // Arrange
        var accession = "ACC";
        var id = 1L;
        ProteinEntry entry = new ProteinEntry();
        entry.setAccession(accession);
        entry.setId(id);
        CrossReference ref = new CrossReference();
        ProteinComment comment = new ProteinComment();
        ProteinPublication pub = new ProteinPublication();
        when(proteinEntryRepository.findAdditionalDetails(accession)).thenReturn(Optional.of(entry));
        when(crossReferenceRepository.findByProteinId(id)).thenReturn(List.of(ref));
        when(proteinCommentRepository.findByProteinId(id)).thenReturn(List.of(comment));
        when(proteinPublicationRepository.findByProteinId(id)).thenReturn(List.of(pub));
        // Act
        Optional<ProteinEntry> result = proteinEntryService.findAdditionalDetails(accession);
        // Assert
        assertThat(result).isPresent();
        ProteinEntry fetched = result.get();
        assertThat(fetched.getCrossReferences()).hasSize(1).contains(ref);
        assertThat(fetched.getComments()).hasSize(1).contains(comment);
        assertThat(fetched.getPublications()).hasSize(1).contains(pub);
        verify(proteinEntryRepository).findAdditionalDetails(accession);
        verify(crossReferenceRepository).findByProteinId(id);
        verify(proteinCommentRepository).findByProteinId(id);
        verify(proteinPublicationRepository).findByProteinId(id);
    }

    @Test
    @DisplayName("Should return empty optional when additional details not found")
    void shouldReturnEmptyWhenAdditionalDetailsNotFound() {
        // Arrange
        var accession = "ACC";
        when(proteinEntryRepository.findAdditionalDetails(accession)).thenReturn(Optional.empty());
        // Act
        Optional<ProteinEntry> result = proteinEntryService.findAdditionalDetails(accession);
        // Assert
        assertThat(result).isEmpty();
        verify(proteinEntryRepository).findAdditionalDetails(accession);
        verifyNoInteractions(crossReferenceRepository, proteinCommentRepository, proteinPublicationRepository);
    }

    @Test
    @DisplayName("Should find all accessions")
    void shouldFindAllAccessions() {
        // Arrange
        List<String> accessions = List.of("P12345", "Q67890");
        when(proteinEntryRepository.findAllAccessions()).thenReturn(accessions);
        // Act
        List<String> result = proteinEntryService.findAllAccessions();
        // Assert
        assertThat(result).isEqualTo(accessions);
        verify(proteinEntryRepository).findAllAccessions();
    }

    @Test
    @DisplayName("Should find all with pageable")
    void shouldFindAllWithPageable() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProteinEntry> page = new PageImpl<>(List.of(new ProteinEntry()));
        when(proteinEntryRepository.findAll(pageable)).thenReturn(page);
        // Act
        Page<ProteinEntry> result = proteinEntryService.findAll(pageable);
        // Assert
        assertThat(result).isEqualTo(page);
        verify(proteinEntryRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should find all with specification and pageable")
    void shouldFindAllWithSpecificationAndPageable() {
        // Arrange
        @SuppressWarnings("unchecked")
        Specification<ProteinEntry> spec = mock(Specification.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProteinEntry> page = new PageImpl<>(List.of(new ProteinEntry()));
        when(proteinEntryRepository.findAll(spec, pageable)).thenReturn(page);
        // Act
        Page<ProteinEntry> result = proteinEntryService.findAll(spec, pageable);
        // Assert
        assertThat(result).isEqualTo(page);
        verify(proteinEntryRepository).findAll(spec, pageable);
    }

    @Test
    @DisplayName("Should count and return count using specification")
    void shouldCountUsingSpecification() {
        // Arrange
        @SuppressWarnings("unchecked")
        Specification<ProteinEntry> spec = mock(Specification.class);
        when(proteinEntryRepository.count(spec)).thenReturn(42L);
        // Act
        long count = proteinEntryService.count(spec);
        // Assert
        assertThat(count).isEqualTo(42L);
        verify(proteinEntryRepository).count(spec);
    }
}

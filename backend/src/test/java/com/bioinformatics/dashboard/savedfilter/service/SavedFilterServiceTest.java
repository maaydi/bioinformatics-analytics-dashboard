package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.exception.AccessDeniedException;
import com.bioinformatics.dashboard.exception.DuplicateFilterNameException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedFilterServiceTest {

    @Mock
    private SavedFilterRepository repository;

    @Mock
    private SavedFilterMapper mapper;

    private SavedFilterService service;

    @BeforeEach
    void setUp() {
        service = new SavedFilterService(repository, mapper);
    }

    @Test
    void listForCurrentUser_returnsMappedDtos() {
        var user = mock(AppUser.class);
        var entity = mock(SavedFilter.class);
        var dto = mock(SavedFilterDto.class);

        when(repository.findByOwnerOrderByCreatedAtDesc(user)).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        var result = service.listForCurrentUser(user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(dto, result.getFirst());
        verify(repository).findByOwnerOrderByCreatedAtDesc(user);
        verify(mapper).toDto(entity);
    }

    @Test
    void create_success_returnsDto() {
        var request = mock(SavedFilterCreateRequest.class);
        when(request.name()).thenReturn("my-filter");
        var owner = mock(AppUser.class);

        var entity = mock(SavedFilter.class);
        var savedEntity = mock(SavedFilter.class);
        var dto = mock(SavedFilterDto.class);

        when(mapper.toEntity(request, owner)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(dto);

        var result = service.create(request, owner);

        assertNotNull(result);
        assertSame(dto, result);
        verify(mapper).toEntity(request, owner);
        verify(repository).save(entity);
        verify(mapper).toDto(savedEntity);
    }

    @Test
    void create_duplicateName_throwsDuplicateFilterNameException() {
        var request = mock(SavedFilterCreateRequest.class);
        when(request.name()).thenReturn("dup-filter");
        var owner = mock(AppUser.class);

        var entity = mock(SavedFilter.class);
        when(mapper.toEntity(request, owner)).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateFilterNameException.class, () -> service.create(request, owner));
        verify(mapper).toEntity(request, owner);
        verify(repository).save(entity);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        var user = mock(AppUser.class);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(42L, user));
        verify(repository).findById(42L);
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_forbidden_throwsAccessDeniedException() {
        var owner = mock(AppUser.class);
        when(owner.getUsername()).thenReturn("owner-user");

        var storedOwner = mock(AppUser.class);
        when(storedOwner.getUsername()).thenReturn("other-user");

        var entity = mock(SavedFilter.class);
        when(entity.getOwner()).thenReturn(storedOwner);

        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        assertThrows(AccessDeniedException.class, () -> service.delete(7L, owner));
        verify(repository).findById(7L);
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_success_deletesEntity() {
        var user = mock(AppUser.class);
        when(user.getUsername()).thenReturn("same-user");

        var storedOwner = mock(AppUser.class);
        when(storedOwner.getUsername()).thenReturn("same-user");

        var entity = mock(SavedFilter.class);
        when(entity.getOwner()).thenReturn(storedOwner);

        when(repository.findById(99L)).thenReturn(Optional.of(entity));

        service.delete(99L, user);

        verify(repository).findById(99L);
        verify(repository).delete(entity);
    }
}


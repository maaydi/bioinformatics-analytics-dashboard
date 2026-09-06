package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.common.exception.AccessDeniedException;
import com.bioinformatics.common.exception.DuplicateFilterNameException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
        var user = new UserPrincipal("test_user", List.of("ROLE_USER"), "");
        var entity = mock(SavedFilter.class);
        var dto = mock(SavedFilterDto.class);

        var expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(repository.findByOwner(user.id(), expectedPageable))
                .thenReturn(new PageImpl<>(List.of(entity), expectedPageable, 1));

        when(mapper.toDto(entity)).thenReturn(dto);

        var result = service.listForCurrentUser(user, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertSame(dto, result.content().getFirst());

        verify(repository).findByOwner(user.id(), expectedPageable);
        verify(mapper).toDto(entity);
    }

    @Test
    void create_success_returnsDto() {
        var request = mock(SavedFilterCreateRequest.class);
        when(request.name()).thenReturn("my-filter");
        var owner = new UserPrincipal("test_user", List.of("ROLE_USER"), "");

        var entity = mock(SavedFilter.class);
        var savedEntity = mock(SavedFilter.class);
        var dto = mock(SavedFilterDto.class);

        when(mapper.toEntity(request, owner.id())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(dto);

        var result = service.create(request, owner);

        assertNotNull(result);
        assertSame(dto, result);
        verify(mapper).toEntity(request, owner.id());
        verify(repository).save(entity);
        verify(mapper).toDto(savedEntity);
    }

    @Test
    void create_duplicateName_throwsDuplicateFilterNameException() {
        var request = mock(SavedFilterCreateRequest.class);
        when(request.name()).thenReturn("dup-filter");
        var owner = new UserPrincipal("test_user", List.of("ROLE_USER"), "");

        var entity = mock(SavedFilter.class);
        when(mapper.toEntity(request, owner.id())).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateFilterNameException.class, () -> service.create(request, owner));
        verify(mapper).toEntity(request, owner.id());
        verify(repository).save(entity);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        var user = new UserPrincipal("test_user", List.of("ROLE_USER"), "");

        assertThrows(ResourceNotFoundException.class, () -> service.delete(42L, user));
        verify(repository).findById(42L);
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_forbidden_throwsAccessDeniedException() {
        var storedOwner = "owner-user";

        var entity = new SavedFilter();
        entity.setOwner(storedOwner);

        var currentUser = new UserPrincipal("other-user", List.of("ROLE_USER"), "");

        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        assertThrows(AccessDeniedException.class, () -> service.delete(7L, currentUser));

        verify(repository).findById(7L);
        verify(repository, never()).delete(any());
    }


    @Test
    void delete_success_deletesEntity() {
        var currentUser = new UserPrincipal("same-user", List.of("ROLE_USER"), "");

        var storedOwner = "same-user";

        var entity = new SavedFilter();
        entity.setId(99L);
        entity.setOwner(storedOwner);

        when(repository.findById(99L)).thenReturn(Optional.of(entity));
        service.delete(99L, currentUser);

        verify(repository).findById(99L);
        verify(repository).deleteById(99L);
    }

}


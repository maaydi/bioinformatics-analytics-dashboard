package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.common.exception.AccessDeniedException;
import com.bioinformatics.common.exception.DuplicateFilterNameException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.bioinformatics.shared.models.security.Constants.ADMIN_ROLE;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterRepository repository;
    private final SavedFilterMapper mapper;

    private static boolean isAdmin(String role) {
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }

    public Optional<SavedFilterDto> getSavedFilterById(long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Cacheable(value = "savedFilters", key = "#username")
    public PagedResponse<SavedFilterDto> listForCurrentUser(String username, int page, int size) {
        log.info("Retrieving saved filter page <{}> for user <{}>", page, username);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var res = repository.findByOwner(username, pageable)
                .map(mapper::toDto);
        return new PagedResponse<>(res.getContent(),
                res.getNumber(),
                res.getSize(),
                res.getTotalElements(),
                res.getTotalPages());
    }

    @CacheEvict(value = "savedFilters", key = "#owner")
    public SavedFilterDto create(SavedFilterCreateRequest request, String owner) {
        log.info("Save filter <{}> created by <{}>", request.name(), owner);
        try {
            var entity = mapper.toEntity(request, owner);
            var res = repository.save(entity);
            log.info("Filter <{}> created by <{}> successfully saved", request.name(), owner);
            return mapper.toDto(res);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateFilterNameException("Duplicated filter name %s".formatted(request.name()), ex);
        } catch (Exception e) {
            log.error("An error occurs while trying to saved filter: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void delete(final Long id, final String username, final String role) {
        log.info("Delete filter <{}> by user <{}>", id, username);
        var filter = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forSavedFilter(id));
        var isOwner = filter.getOwner().equals(username);
        log.info("filter owner {} , user {}", filter.getOwner(), username);
        log.info("is owner {} ", isOwner);
        log.info("is admin {}", isAdmin(role));
        if (!isOwner && !isAdmin(role)) {
            throw new AccessDeniedException("You don't have permission to delete this filter");
        }
        deleteAndEvict(filter.getId(), filter.getOwner());
    }

    @CacheEvict(value = "savedFilters", key = "#owner")
    public void deleteAndEvict(Long filterId, String owner) {
        log.info("Delete filter ID <{}> and clear cache for its owner <{}>", filterId, owner);
        try {
            repository.deleteById(filterId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.exception.AccessDeniedException;
import com.bioinformatics.dashboard.exception.DuplicateFilterNameException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterRepository repository;
    private final SavedFilterMapper mapper;

    @Cacheable(value = "savedFilters", key = "#currentUser.id")
    public PagedResponse<SavedFilterDto> listForCurrentUser(AppUser currentUser, int page, int size) {
        log.info("Retrieving saved filter page <{}> for user <{}>", page, currentUser.getUsername());
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var res = repository.findByOwner(currentUser, pageable)
                .map(mapper::toDto);
        return new PagedResponse<>(res.getContent(),
                res.getNumber(),
                res.getSize(),
                res.getTotalElements(),
                res.getTotalPages());
    }

    @CacheEvict(value = "savedFilters", key = "#owner.id")
    public SavedFilterDto create(SavedFilterCreateRequest request, AppUser owner) {
        log.info("Save filter <{}> created by <{}>", request.name(), owner.getUsername());
        try {
            var entity = mapper.toEntity(request, owner);
            var res = repository.save(entity);
            log.info("Filter <{}> created by <{}> successfully saved", request.name(), owner.getUsername());
            return mapper.toDto(res);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateFilterNameException("Duplicated filter name %s".formatted(request.name()), ex);
        } catch (Exception e) {
            log.error("An error occurs while trying to saved filter: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        log.info("Delete filter <{}> by user <{}>", id, currentUser.getUsername());
        var filter = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forSavedFilter(id));
        var isOwner = filter.getOwner().getUsername().equals(currentUser.getUsername());
        if (!isOwner && !currentUser.isAdmin()) {
            throw new AccessDeniedException("You don't have permission to delete this filter");
        }
        deleteAndEvict(filter.getId(), filter.getOwner());
    }

    @CacheEvict(value = "savedFilters", key = "#owner.id", cacheManager = "pageResponseSavedFiltersCacheManager")
    public void deleteAndEvict(Long filterId, AppUser owner) {
        log.info("Delete filter ID <{}> and clear cache for its owner <{}>", filterId, owner.getUsername());
        try {
            repository.deleteById(filterId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

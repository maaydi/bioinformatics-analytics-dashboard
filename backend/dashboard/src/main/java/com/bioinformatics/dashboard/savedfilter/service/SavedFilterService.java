package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.common.exception.AccessDeniedException;
import com.bioinformatics.common.exception.DuplicateFilterNameException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import com.bioinformatics.shared.models.security.UserPrincipal;
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

    @Cacheable(value = "savedFilters", key = "#user.id")
    public PagedResponse<SavedFilterDto> listForCurrentUser(UserPrincipal user, int page, int size) {
        log.info("Retrieving saved filter page <{}> for user <{}>", page, user.id());
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var res = repository.findByOwner(user.id(), pageable)
                .map(mapper::toDto);
        return new PagedResponse<>(res.getContent(),
                res.getNumber(),
                res.getSize(),
                res.getTotalElements(),
                res.getTotalPages());
    }

    @CacheEvict(value = "savedFilters", key = "#owner.id")
    public SavedFilterDto create(SavedFilterCreateRequest request, UserPrincipal owner) {
        log.info("Save filter <{}> created by <{}>", request.name(), owner);
        try {
            var entity = mapper.toEntity(request, owner.id());
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
    public void delete(final Long id, final UserPrincipal user) {
        log.info("Delete filter <{}> by user <{}>", id, user.id());
        var filter = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forSavedFilter(id));
        var isOwner = filter.getOwner().equals(user.id());
        if (!isOwner && !user.isAdmin()) {
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

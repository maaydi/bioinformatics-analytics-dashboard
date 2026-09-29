package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.common.exception.AccessDeniedException;
import com.bioinformatics.common.exception.DuplicateFilterNameException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.filter.SavedFilterDto;
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

/**
 * Service layer for persisting and retrieving user-scoped saved gene-search filters.
 *
 * <p>This service is responsible for validating ownership, enforcing uniqueness constraints on filter names,
 * and exposing paginated access to the current user's saved analytical states. The persisted payload is a
 * {@link com.bioinformatics.common.models.gene.GeneSearchRequest}, allowing a saved filter to be reused as a
 * searchable gene-query snapshot.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterRepository repository;
    private final SavedFilterMapper mapper;

    private static boolean isAdmin(String role) {
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }

    /**
     * Loads a saved filter by its database identifier.
     *
     * @param id unique saved-filter identifier
     * @return optional DTO containing the persisted filter when found
     */
    public Optional<SavedFilterDto> getSavedFilterById(long id) {
        log.debug("[DASHBOARD][FILTER] Fetching saved filter id={}", id);
        return repository.findById(id).map(mapper::toDto);
    }

    /**
     * Lists all saved filters owned by the specified user in descending creation order.
     *
     * @param user authenticated user requesting the list
     * @param page zero-based page index
     * @param size page size
     * @return paginated list of saved filters for the owner
     */
    @Cacheable(value = "savedFilters", key = "#user.id")
    public PagedResponse<SavedFilterDto> listForCurrentUser(UserPrincipal user, int page, int size) {
        log.info("[DASHBOARD][FILTER] Retrieving saved filter page={} size={} for user={}", page, size, user.id());
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var res = repository.findByOwner(user.id(), pageable)
                .map(mapper::toDto);
        return new PagedResponse<>(res.getContent(),
                res.getNumber(),
                res.getSize(),
                res.getTotalElements(),
                res.getTotalPages());
    }

    /**
     * Persists a new named gene-search filter for the current user.
     *
     * @param request request payload containing filter name and filter JSON
     * @param owner   authenticated user creating the filter
     * @return persisted filter DTO
     * @throws DuplicateFilterNameException when the same user already has a filter with the same name
     */
    @CacheEvict(value = "savedFilters", key = "#owner.id")
    public SavedFilterDto create(SavedFilterCreateRequest request, UserPrincipal owner) {
        log.info("[DASHBOARD][FILTER] Save filter requested name={} user={}", request.name(), owner.id());
        try {
            var entity = mapper.toEntity(request, owner.id());
            var res = repository.save(entity);
            log.info("[DASHBOARD][FILTER] Filter saved successfully id={} name={} user={}", res.getId(), request.name(), owner.id());
            return mapper.toDto(res);
        } catch (DataIntegrityViolationException ex) {
            log.warn("[DASHBOARD][FILTER] Duplicate filter name rejected name={} user={}", request.name(), owner.id());
            throw new DuplicateFilterNameException("Duplicated filter name %s".formatted(request.name()), ex);
        } catch (Exception e) {
            log.error("[DASHBOARD][FILTER] Failed to save filter name={} user={} error={}", request.name(), owner.id(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a saved filter after validating ownership or admin privileges.
     *
     * @param id saved filter identifier
     * @param user authenticated caller
     * @throws ResourceNotFoundException when the filter does not exist
     * @throws AccessDeniedException when the user is not the owner and is not an admin
     */
    @Transactional
    public void delete(final Long id, final UserPrincipal user) {
        log.info("[DASHBOARD][FILTER] Delete filter requested id={} user={}", id, user.id());
        var filter = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forSavedFilter(id));
        var isOwner = filter.getOwner().equals(user.id());
        if (!isOwner && !user.isAdmin()) {
            log.warn("[DASHBOARD][FILTER] Access denied deleting filter id={} user={} owner={}", id, user.id(), filter.getOwner());
            throw new AccessDeniedException("You don't have permission to delete this filter");
        }
        deleteAndEvict(filter.getId(), filter.getOwner());
    }

    /**
     * Deletes the entity and evicts the user-scoped cache entry.
     *
     * @param filterId saved filter identifier
     * @param owner owner username used to build the cache key
     */
    @CacheEvict(value = "savedFilters", key = "#owner")
    public void deleteAndEvict(Long filterId, String owner) {
        log.info("[DASHBOARD][FILTER] Deleting filter id={} owner={} and evicting cache", filterId, owner);
        try {
            repository.deleteById(filterId);
            log.info("[DASHBOARD][FILTER] Filter deleted successfully id={} owner={}", filterId, owner);
        } catch (Exception e) {
            log.error("[DASHBOARD][FILTER] Failed to delete filter id={} owner={} error={}", filterId, owner, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

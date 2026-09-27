package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that persists and manages user-scoped gene-search snapshots.
 *
 * <p>Saved filters allow authenticated users to store a reusable {@link com.bioinformatics.common.models.gene.GeneSearchRequest}
 * under a human-readable name. The saved state can later be reloaded and re-applied to the gene search endpoints,
 * enabling repeatable analytical workflows without re-entering all filter criteria.</p>
 *
 * <p>All endpoints are restricted to authenticated users with either the USER or ADMIN role and the service layer
 * enforces ownership checks before mutating or deleting records.</p>
 */
@RestController
@RequestMapping("/api/saved-filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@Slf4j
public class SavedFilterController {

    private final SavedFilterService service;

    /**
     * Lists the saved filters owned by the currently authenticated user.
     *
     * @param page zero-based page index
     * @param size page size, limited to 1..200
     * @param user authenticated caller resolved from the request context
     * @return paginated saved filter list for the current user
     */
    @GetMapping
    @Auditable(action = AuditAction.FILTER_SAVE)
    @RateLimited
    public PagedResponse<SavedFilterDto> listSavedFilters(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal user) {
        log.info("[DASHBOARD][FILTER] Listing saved filters for user={} page={} size={}", user.id(), page, size);
        return service.listForCurrentUser(user, page, size);
    }

    /**
     * Creates a new saved filter instance for the current user.
     *
     * @param request request payload containing filter name and gene-search snapshot
     * @param user    authenticated caller
     * @return persisted saved filter DTO with HTTP 201 Created
     */
    @PostMapping
    @Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result.id")
    @RateLimited
    public ResponseEntity<SavedFilterDto> createSavedFilter(@Valid @RequestBody SavedFilterCreateRequest request, @CurrentUser UserPrincipal user) {
        log.info("[DASHBOARD][FILTER] Creating saved filter name={} user={}", request.name(), user.id());
        var res = service.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * Retrieves a single saved filter by identifier.
     *
     * @param id persisted filter identifier
     * @return matching filter DTO or HTTP 404 when the record does not exist
     */
    @GetMapping("/{id}")
    @Auditable(action = AuditAction.FILTER_LOAD, targetId = "#id")
    @RateLimited
    public ResponseEntity<SavedFilterDto> getSavedFilterById(@PathVariable Long id) {
        log.info("[DASHBOARD][FILTER] Loading saved filter id={}", id);
        return service.getSavedFilterById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a saved filter if the current user owns it or has admin privileges.
     *
     * @param id persisted filter identifier
     * @param user authenticated caller
     * @return HTTP 204 No Content after successful deletion
     */
    @DeleteMapping("/{id}")
    @Auditable(action = AuditAction.FILTER_DELETE, targetId = "#id")
    @RateLimited
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, @CurrentUser UserPrincipal user) {
        log.info("[DASHBOARD][FILTER] Deleting saved filter id={} user={}", id, user.id());
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}

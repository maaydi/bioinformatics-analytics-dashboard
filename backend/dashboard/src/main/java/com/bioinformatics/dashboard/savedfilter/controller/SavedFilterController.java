package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller responsible for persisting and managing user-specific filter variants.
 *
 * <p>Allows authenticated users to save their current complex filter state and retrieve it
 * later, enabling reproducible analytic workflows.</p>
 * <ul>
 *   <li>{@code GET    /api/saved-filters}      — lists paginated saved filters belonging to the active user</li>
 *   <li>{@code POST   /api/saved-filters}      — stores a new named snapshot of the current filter parameters</li>
 *   <li>{@code DELETE /api/saved-filters/{id}} — safely destroys a saved filter belonging to the issuer</li>
 * </ul>
 *
 * <p>Security dictates that standard users can interact only with their own saved configurations
 * scoped per {@code AppUser.id}. Handled functionally by {@link SavedFilterService}.</p>
 */
@RestController
@RequestMapping("/api/saved-filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class SavedFilterController {

    private final SavedFilterService service;

    @GetMapping
    @Auditable(action = AuditAction.FILTER_SAVE)
    @RateLimited
    public PagedResponse<SavedFilterDto> listSavedFilters(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal user) {
        return service.listForCurrentUser(user, page, size);
    }

    @PostMapping
    @Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result.id")
    @RateLimited
    public ResponseEntity<SavedFilterDto> createSavedFilter(@Valid @RequestBody SavedFilterCreateRequest request, @CurrentUser UserPrincipal user) {
        var res = service.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{id}")
    @Auditable(action = AuditAction.FILTER_LOAD, targetId = "#id")
    @RateLimited
    public ResponseEntity<SavedFilterDto> getSavedFilterById(@PathVariable Long id) {
        return service.getSavedFilterById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Auditable(action = AuditAction.FILTER_DELETE, targetId = "#id")
    @RateLimited
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, @CurrentUser UserPrincipal user) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}

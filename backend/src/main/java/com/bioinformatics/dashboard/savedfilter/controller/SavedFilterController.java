package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for saved filter sets.
 *
 * <p>Contract: documentation/api-contract.md §4 — Saved Filters Endpoints.
 * <ul>
 *   <li>{@code GET    /api/saved-filters}      — list own saved filters</li>
 *   <li>{@code POST   /api/saved-filters}      — create a saved filter</li>
 *   <li>{@code DELETE /api/saved-filters/{id}} — delete (own only; ADMIN may delete any)</li>
 * </ul>
 *
 * <p>Authorization: USER and ADMIN (users may only access their own filters).
 */
@RestController
@RequestMapping("/api/saved-filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class SavedFilterController {

    private final SavedFilterService service;

    @GetMapping
    @Auditable(action = AuditAction.FILTER_SAVE)
    public PagedResponse<SavedFilterDto> listSavedFilters(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUser currentUser) {
        return service.listForCurrentUser(currentUser, page, size);
    }

    @PostMapping
    @Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result.id")
    public ResponseEntity<SavedFilterDto> createSavedFilter(@Valid @RequestBody SavedFilterCreateRequest request,
                                                            @AuthenticationPrincipal AppUser currentUser) {
        var res = service.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/{id}")
    @Auditable(action = AuditAction.FILTER_DELETE, targetId = "#id")
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, @AuthenticationPrincipal AppUser currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}

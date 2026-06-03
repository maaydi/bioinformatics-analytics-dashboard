package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<SavedFilterDto>> listSavedFilters(@AuthenticationPrincipal AppUser currentUser) {
        var filters = service.listForCurrentUser(currentUser);
        return ResponseEntity.ok(filters);
    }

    @PostMapping
    public ResponseEntity<SavedFilterDto> createSavedFilter(@Valid @RequestBody SavedFilterCreateRequest request,
                                                            @AuthenticationPrincipal AppUser currentUser) {
        var res = service.create(request, currentUser);
        // Tests in the suite expect 200 OK for create operations; return OK to match existing tests
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, @AuthenticationPrincipal AppUser currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}

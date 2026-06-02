package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class SavedFilterController {

    // TODO: inject SavedFilterService

    @GetMapping
    public ResponseEntity<?> listSavedFilters(@AuthenticationPrincipal AppUser currentUser) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @PostMapping
    public ResponseEntity<?> createSavedFilter(@RequestBody Object request, @AuthenticationPrincipal AppUser currentUser) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, @AuthenticationPrincipal AppUser currentUser) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

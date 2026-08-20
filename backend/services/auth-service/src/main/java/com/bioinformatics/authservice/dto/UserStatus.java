package com.bioinformatics.authservice.dto;

import com.bioinformatics.authservice.entity.AppUser;

/**
 * Lifecycle states for an {@link AppUser}.
 *
 * <p>Soft-delete strategy: accounts transition to {@code DELETED} instead of
 * being physically removed, preserving audit history.
 */
public enum UserStatus {
    /**
     * Account created but not yet confirmed / first-login done.
     */
    CREATED,
    /**
     * Normal operating state.
     */
    ACTIVE,
    /**
     * Manually suspended by an admin.
     */
    DISABLED,
    /**
     * Soft-deleted — excluded from all standard queries.
     */
    DELETED
}


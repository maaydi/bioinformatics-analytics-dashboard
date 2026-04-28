package com.bioinformatics.dashboard.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user details from {@code app_user} table for Spring Security authentication.
 *
 * <p>Implementation will use the {@code AppUserRepository} once the auth feature
 * is fully implemented (Phase 1 / ticket AUTH-001).
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    // TODO: inject AppUserRepository when auth feature is implemented

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: implement — load from app_user table
        throw new UsernameNotFoundException("User not found: " + username);
    }
}

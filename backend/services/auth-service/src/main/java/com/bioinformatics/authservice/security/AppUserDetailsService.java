package com.bioinformatics.authservice.security;

import com.bioinformatics.authservice.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService implementation.
 *
 * <p>Loads user accounts from the auth schema for authentication and authorization.
 *
 * <p>Called by:
 * <ul>
 *   <li>AuthenticationManager during login
 *   <li>JwtService during token validation
 *   <li>Spring Security context for authorization checks
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>No usernames or user data logged (privacy)
 *   <li>UsernameNotFoundException thrown for non-existent users (prevents enumeration)
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    /**
     * Loads user by username for Spring Security authentication.
     *
     * <p>Called during login and token validation to retrieve user authorities and password hash.
     *
     * @param username user identifier
     * @return AppUser (implements UserDetails)
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public @NonNull UserDetails loadUserByUsername(final @NonNull String username) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(username)));
    }
}


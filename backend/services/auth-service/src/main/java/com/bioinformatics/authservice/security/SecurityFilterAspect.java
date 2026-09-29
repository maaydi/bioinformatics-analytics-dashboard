package com.bioinformatics.authservice.security;

import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security audit and soft-delete enforcement aspect.
 *
 * <p>This aspect configures Hibernate filters so that soft-deleted users are excluded
 * from normal repository queries unless the caller is an admin. This preserves the
 * principle of least privilege while still allowing administrators to inspect deleted users.
 *
 * <p>Security model:
 * <ul>
 *   <li>Regular users see only active/non-deleted accounts</li>
 *   <li>Admins can see deleted users when explicitly required</li>
 *   <li>Filter is applied centrally, avoiding scattered condition checks</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
public class SecurityFilterAspect {

    private final EntityManager entityManager;

    public SecurityFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Configures the soft-delete Hibernate filter before service methods run.
     *
     * <p>If the current user is admin, the filter is disabled so deleted users can be inspected.
     * Otherwise, non-deleted users are enforced by default.
     */
    @Before("execution(* com.bioinformatics.*.service.*.*(..))")
    public void configureFilters() {
        var session = entityManager.unwrap(Session.class);
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal user) {
            if (user.isAdmin()) {
                log.debug("[AUTH_FILTER] Admin request detected - excluding soft-delete filter");
                session.enableFilter("excludeDeletedFilter")
                        .setParameter("isDeletedExcluded", false);
                return;
            }
        }

        log.debug("[AUTH_FILTER] Regular user request detected - enforcing non-deleted user filter");
        session.enableFilter("excludeDeletedFilter")
                .setParameter("isDeletedExcluded", true);
    }
}

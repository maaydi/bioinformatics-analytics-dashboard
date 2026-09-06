package com.bioinformatics.authservice.security;

import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecurityFilterAspect {

    private final EntityManager entityManager;

    public SecurityFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* com.bioinformatics.*.service.*.*(..))")
    public void configureFilters() {
        var session = entityManager.unwrap(Session.class);
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal user) {
            if (user.isAdmin()) {
                session.enableFilter("excludeDeletedFilter")
                        .setParameter("isDeletedExcluded", false);
                return;
            }
        }

        session.enableFilter("excludeDeletedFilter")
                .setParameter("isDeletedExcluded", true);
    }
}

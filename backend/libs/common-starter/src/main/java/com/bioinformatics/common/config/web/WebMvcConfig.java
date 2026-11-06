package com.bioinformatics.common.config.web;

import com.bioinformatics.common.exception.GlobalExceptionHandler;
import com.bioinformatics.common.providers.ProviderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC registration for the shared web argument resolvers.
 *
 * <p>This config wires the custom {@link CurrentUserArgumentResolver} into Spring MVC so controllers
 * can receive the authenticated {@link com.bioinformatics.shared.models.security.UserPrincipal}
 * directly via the {@link CurrentUser} annotation.
 */
@AutoConfiguration
@ConditionalOnClass({WebMvcConfigurer.class, org.springframework.security.core.context.SecurityContextHolder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "common", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@Import({AuthenticatedUserResolver.class, CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, ProviderFilter.class})
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}

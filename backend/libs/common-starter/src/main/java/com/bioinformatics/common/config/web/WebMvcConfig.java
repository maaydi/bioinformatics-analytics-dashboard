package com.bioinformatics.common.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}

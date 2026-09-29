package com.bioinformatics.common.config.web;

import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves controller method arguments annotated with {@link CurrentUser}.
 *
 * <p>This resolver extracts the current {@link UserPrincipal} from the Spring Security context,
 * allowing controllers to receive the authenticated identity without manual security lookups.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticatedUserResolver userResolver;

    /**
     * Returns true only for controller parameters annotated with {@link CurrentUser} and of type {@link UserPrincipal}.
     *
     * @param parameter controller method parameter
     * @return true when the parameter should be resolved here
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UserPrincipal.class);
    }

    /**
     * Resolves the current authenticated user and injects it into the controller argument.
     *
     * @param parameter     controller method parameter
     * @param mavContainer  model and view container
     * @param webRequest    native web request
     * @param binderFactory binder factory
     * @return authenticated user principal
     * @throws BadCredentialsException if the user is not authenticated
     */
    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var user = userResolver.getCurrentUser();
        if (user.isEmpty()) {
            log.warn("[COMMON_AUTH] CurrentUser injection failed - no authenticated principal in security context");
            throw new BadCredentialsException("User is not logged in");
        }

        log.debug("[COMMON_AUTH] CurrentUser resolved for controller argument - userId='{}', roles={}",
                user.get().id(), user.get().roles());
        return user.get();
    }
}
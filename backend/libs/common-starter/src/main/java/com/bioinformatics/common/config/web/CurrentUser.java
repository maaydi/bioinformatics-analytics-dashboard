package com.bioinformatics.common.config.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter that should be injected with the currently authenticated
 * {@link com.bioinformatics.shared.models.security.UserPrincipal}.
 *
 * <p>Use this annotation on parameters where the service code wants the active identity without
 * directly reading the security context or HTTP request headers.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

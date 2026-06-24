package com.bioinformatics.dashboard.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /**
     * The key referencing the configuration in application.yml.
     * Defaults to "global" if not specified.
     */
    String key() default "global";
}

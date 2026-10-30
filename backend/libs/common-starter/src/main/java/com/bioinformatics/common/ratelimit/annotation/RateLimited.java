package com.bioinformatics.common.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /**
     * Policy name under {@code app.rate-limiter}. Methods with the same key
     * share one per-client-IP budget within each service instance.
     */
    String key() default "global";
}

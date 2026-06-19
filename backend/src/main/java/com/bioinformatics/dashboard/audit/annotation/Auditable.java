package com.bioinformatics.dashboard.audit.annotation;

import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /**
     * Action type being audited (e.g., "LOGIN", "FILTER_SAVE")
     */
    AuditAction action();

    /**
     * Target resource type (e.g., "AUTH", "SAVED_FILTER")
     */
    AuditTarget target() default AuditTarget.USER;

    /**
     * Whether to audit only on failure (for high-volume operations)
     */
    boolean auditOnlyOnFailure() default false;

    /**
     * Whether to skip audit for this method
     */
    boolean skip() default false;
}

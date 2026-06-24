package com.bioinformatics.dashboard.audit.service;

import com.bioinformatics.dashboard.audit.dto.AuditWebDetails;

public final class AuditContextHolder {
    private static final ThreadLocal<AuditWebDetails> CONTEXT = new ThreadLocal<>();

    private AuditContextHolder() {
    }


    /**
     * Set the audit web details for the current thread.
     *
     * @param details the AuditWebDetails to associate with the current thread
     */
    public static void set(AuditWebDetails details) {
        CONTEXT.set(details);
    }

    /**
     * Get the audit web details associated with the current thread.
     *
     * @return the AuditWebDetails or {@code null} if none set
     */
    public static AuditWebDetails get() {
        return CONTEXT.get();
    }

    /**
     * Clear the audit web details for the current thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
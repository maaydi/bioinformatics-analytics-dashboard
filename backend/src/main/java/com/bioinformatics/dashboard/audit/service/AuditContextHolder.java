package com.bioinformatics.dashboard.audit.service;

import com.bioinformatics.dashboard.audit.dto.AuditWebDetails;

public final class AuditContextHolder {
    private static final ThreadLocal<AuditWebDetails> CONTEXT = new ThreadLocal<>();

    private AuditContextHolder() {
    }


    public static void set(AuditWebDetails details) {
        CONTEXT.set(details);
    }

    public static AuditWebDetails get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
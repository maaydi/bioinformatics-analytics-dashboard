package com.bioinformatics.common.correlation;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public final class CorrelationContext {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    private CorrelationContext() {
    }

    public static String normalize(String value) {
        if (value != null && VALID_ID.matcher(value).matches()) {
            return value;
        }
        if (value != null) {
            log.warn("Replacing invalid correlation ID (length={})", value.length());
        }
        return UUID.randomUUID().toString();
    }

    public static String currentOrNew() {
        return normalize(MDC.get(MDC_KEY));
    }

    public static Scope open(String value) {
        var previous = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, normalize(value));
        return () -> {
            if (previous == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previous);
            }
        };
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}

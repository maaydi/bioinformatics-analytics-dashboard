package com.bioinformatics.common.correlation;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;

public class CorrelationRecordInterceptor implements RecordInterceptor<String, Object> {
    private final ThreadLocal<CorrelationContext.Scope> scope = new ThreadLocal<>();

    @Override
    public ConsumerRecord<String, Object> intercept(ConsumerRecord<String, Object> record,
                                                    @NonNull Consumer<String, Object> consumer) {
        var header = record.headers().lastHeader(CorrelationContext.HEADER);
        scope.set(CorrelationContext.open(header == null || header.value() == null ? null
                : new String(header.value(), StandardCharsets.UTF_8)));
        return record;
    }

    @Override
    public void afterRecord(@NonNull ConsumerRecord<String, Object> record, @NonNull Consumer<String, Object> consumer) {
        var current = scope.get();
        if (current != null) {
            current.close();
            scope.remove();
        }
    }
}

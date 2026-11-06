package com.bioinformatics.common.correlation;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CorrelationProducerInterceptor implements ProducerInterceptor<String, Object> {
    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        record.headers().remove(CorrelationContext.HEADER);
        record.headers().add(CorrelationContext.HEADER,
                CorrelationContext.currentOrNew().getBytes(StandardCharsets.UTF_8));
        return record;
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}

package com.bioinformatics.common.config.kafka;


import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.correlation.CorrelationProducerInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;

/**
 * {@link KafkaTemplate} configured with JSON value serialization so that
 * domain events (POJOs / Records) are published without manual mapping.
 * <p>Consumers select their own explicit event type; Java type headers are never sent.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@RequiredArgsConstructor
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "common", name = {"enabled", "kafka.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "common.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(CommonProperties.class)
public class KafkaProducerConfig {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, Object> producerFactory(
            @Value("${common.correlation.enabled:true}") boolean correlationEnabled) {
        var props = new HashMap<String, Object>();
        var producer = commonProperties.kafka().producer();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, commonProperties.kafka().bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, producer.acks());
        props.put(ProducerConfig.RETRIES_CONFIG, producer.retries());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.batchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, producer.lingerMs());
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        if (correlationEnabled) {
            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, CorrelationProducerInterceptor.class.getName());
        }

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}

package com.bioinformatics.common.config.kafka;


import com.bioinformatics.common.config.CommonProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;

/**
 * {@link KafkaTemplate} configured with JSON value serialization so that
 * domain events (POJOs / Records) are published without manual mapping.
 * <p>Type info is added to headers to allow safe polymorphic consumption.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "common.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(CommonProperties.class)
public class KafkaProducerConfig {

    private final CommonProperties commonProperties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        var props = new HashMap<String, Object>();
        var producer = commonProperties.kafka().producer();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, commonProperties.kafka().bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, producer.acks());
        props.put(ProducerConfig.RETRIES_CONFIG, producer.retries());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.batchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, producer.lingerMs());
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

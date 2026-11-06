package com.bioinformatics.common.config.kafka;

import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.correlation.CorrelationRecordInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.StringUtils;

import java.util.HashMap;

@AutoConfiguration(beforeName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@RequiredArgsConstructor
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
@ConditionalOnProperty(prefix = "common", name = {"enabled", "kafka.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "common.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(CommonProperties.class)
public class KafkaConsumerConfig {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            @Value("${common.correlation.enabled:true}") boolean correlationEnabled,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(commonProperties.kafka().consumer().concurrency());
        factory.setBatchListener(commonProperties.kafka().consumer().batchListener());
        factory.setAutoStartup(autoStartup);
        var ack = ContainerProperties.AckMode.valueOf(commonProperties.kafka().consumer().ackMode().toUpperCase());
        factory.getContainerProperties().setAckMode(ack);
        if (correlationEnabled) {
            factory.setRecordInterceptor(new CorrelationRecordInterceptor());
        }
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    public DefaultKafkaConsumerFactory<String, Object> consumerFactory() {
        var props = new HashMap<String, Object>();
        var consumer = commonProperties.kafka().consumer();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, commonProperties.kafka().bootstrapServers());
        if (StringUtils.hasText(consumer.groupId())) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.groupId());
        }
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.autoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }
}

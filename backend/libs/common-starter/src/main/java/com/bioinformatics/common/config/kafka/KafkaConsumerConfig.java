package com.bioinformatics.common.config.kafka;

import com.bioinformatics.common.config.CommonProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
@ConditionalOnProperty(prefix = "common.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(CommonProperties.class)
public class KafkaConsumerConfig {

    private final CommonProperties commonProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(commonProperties.kafka().consumer().concurrency());
        factory.setBatchListener(commonProperties.kafka().consumer().batchListener());
        var ack = ContainerProperties.AckMode.valueOf(commonProperties.kafka().consumer().ackMode().toUpperCase());
        factory.getContainerProperties().setAckMode(ack);
        return factory;
    }

    @Bean
    public DefaultKafkaConsumerFactory<String, Object> consumerFactory() {
        var props = new HashMap<String, Object>();
        var consumer = commonProperties.kafka().consumer();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, commonProperties.kafka().bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.groupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.autoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class.getName());
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.bioinformatics.*");

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(Object.class)));
    }
}

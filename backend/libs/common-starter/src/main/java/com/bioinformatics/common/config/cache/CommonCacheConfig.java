package com.bioinformatics.common.config.cache;

import com.bioinformatics.common.config.CommonProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.type.TypeFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

@Configuration
@EnableCaching
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnClass({RedisConnectionFactory.class, RedisCacheManager.class})
@ConditionalOnProperty(prefix = "common.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CommonProperties.class)
public class CommonCacheConfig {

    private final CommonProperties commonProperties;


    private ObjectMapper getBaseMapper(DefaultTyping typing) {
        var ptvBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("java.util")
                .allowIfBaseType(java.lang.Object.class);

        // Dynamically add allowed packages
        for (String pkg : commonProperties.cache().allowedBasePackages()) {
            ptvBuilder.allowIfBaseType(pkg);
        }

        return JsonMapper.builder()
                .activateDefaultTyping(ptvBuilder.build(), typing, JsonTypeInfo.As.PROPERTY)
                .build();
    }

    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            List<CacheRegistryProvider> cacheProviders) {

        var baseMapper = getBaseMapper(DefaultTyping.JAVA_LANG_OBJECT);

        var defaultGlobalConfig = createBaseConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJacksonJsonRedisSerializer(baseMapper)));

        var builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultGlobalConfig);

        // Dynamically register typed caches defined by the host microservice
        if (cacheProviders != null) {
            for (CacheRegistryProvider provider : cacheProviders) {
                for (var spec : provider.getCacheSpecs()) {
                    registerTypedCache(builder, baseMapper, spec.parameterizedType(), spec.elementType(), spec.cacheNames());
                }
            }
        }

        return builder.build();
    }

    /**
     * Fallback cache manager for unconfigured caches containing Java Records or non-final types.
     * Uses NON_FINAL_AND_RECORDS typing strategy when the primary manager's strict JAVA_LANG_OBJECT mode is too restrictive.
     */
    @Bean
    public RedisCacheManager redisNonFinalAndRecordCacheManager(RedisConnectionFactory connectionFactory) {
        var nonFinalMapper = getBaseMapper(DefaultTyping.NON_FINAL_AND_RECORDS);
        var baseSerializer = RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJacksonJsonRedisSerializer(nonFinalMapper));
        var config = createBaseConfig()
                .serializeValuesWith(baseSerializer);
        var customSpecs = new HashMap<String, RedisCacheConfiguration>();
        customSpecs.put("refresh-tokens", createBaseConfig()
                .serializeValuesWith(baseSerializer)
                .entryTtl(Duration.ofSeconds(commonProperties.jwt().accessTokenExpirySeconds())));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(customSpecs)
                .build();
    }

    private RedisCacheConfiguration createBaseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.parse(commonProperties.cache().entryTtlDuration()))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
    }

    private void registerTypedCache(RedisCacheManager.RedisCacheManagerBuilder builder,
                                    ObjectMapper mapper,
                                    Class<?> parametrizedType,
                                    Class<?> elementType,
                                    String... cacheNames) {
        var targetType = TypeFactory.createDefaultInstance().constructParametricType(parametrizedType, elementType);
        var serializer = new JacksonJsonRedisSerializer<>(mapper, targetType);
        var cacheConfig = createBaseConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        for (String name : cacheNames) {
            builder.withCacheConfiguration(name, cacheConfig);
        }
    }
}
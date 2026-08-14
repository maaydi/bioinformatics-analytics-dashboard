package com.bioinformatics.dashboard.config;

import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Configures Redis caching for analytics queries, search results, and saved filters.
 * Uses polymorphic Jackson serialization to handle DTO hierarchies safely across cache layers.
 * 6-hour TTL with per-cache typed serializers for consistent deserialization.
 */
@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig {


    @Value("${app.jwt.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds;


    /**
     * Creates an ObjectMapper with polymorphic type handling for safe Redis serialization.
     * Restricts allowed base types to application DTOs and standard Java collections.
     */
    private static ObjectMapper getBaseMapper(DefaultTyping typing) {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.bioinformatics.dashboard")
                .allowIfBaseType("java.util")
                .allowIfBaseType(java.lang.Object.class)
                .build();
        return JsonMapper.builder()
                .activateDefaultTyping(ptv, typing, JsonTypeInfo.As.PROPERTY)
                .build();
    }

    /**
     * Primary cache manager with typed serialization for all DTO payloads.
     * Prevents Jackson type confusion between cached analytics, search, and filter results.
     */
    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var baseMapper = getBaseMapper(DefaultTyping.JAVA_LANG_OBJECT);

        var defaultGlobalConfig = createBaseConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJacksonJsonRedisSerializer(baseMapper)));

        var builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultGlobalConfig);

        registerTypedCache(builder, baseMapper, List.class, OrganismCountDto.class, "byOrganism", "filtered-byOrganism");
        registerTypedCache(builder, baseMapper, List.class, LengthHistogramBucketDto.class, "lengthHistogram", "filtered-lengthHistogram");
        registerTypedCache(builder, baseMapper, PagedResponse.class, SavedFilterDto.class, "savedFilters");
        registerTypedCache(builder, baseMapper, PagedResponse.class, ProteinSummaryDto.class, "geneList", "geneSearch", "geneList-kb", "geneSearch-kb");
        registerTypedCache(builder, baseMapper, List.class, ReviewedRatioDto.class, "reviewedRatio", "filtered-reviewedRatio");
        registerTypedCache(builder, baseMapper, List.class, EvidenceDistributionDto.class, "evidenceLevels", "filtered-evidenceLevels");
        registerTypedCache(builder, baseMapper, List.class, KeywordFrequencyDto.class, "keywordFrequency", "filtered-keywordFrequency");
        registerTypedCache(builder, baseMapper, List.class, ProteinLengthWeightCount.class, "filtered-proteinLengthWeightCount");
        registerTypedCache(builder, baseMapper, List.class, String.class, "geneKeywords");

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
                .entryTtl(Duration.ofSeconds(accessTokenExpirySeconds)));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(customSpecs)
                .build();
    }

    /**
     * Base cache configuration: 6-hour TTL, null-value rejection, string-key serialization.
     * Applied to all caches unless overridden with typed serializers.
     */
    private RedisCacheConfiguration createBaseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
    }

    /**
     * Registers a typed cache with Jackson serialization for a specific DTO parameterized type.
     * Ensures correct deserialization of generic containers (e.g., {@code List<DtoType>}, {@code PagedResponse<T>}).
     *
     * @param builder          cache manager builder
     * @param mapper           polymorphic ObjectMapper
     * @param parametrizedType container class (e.g., List. Class, PagedResponse. Class)
     * @param elementType      element type for parameterization
     * @param cacheNames       cache names to register with this typed configuration
     */
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
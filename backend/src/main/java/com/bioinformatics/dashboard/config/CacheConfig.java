package com.bioinformatics.dashboard.config;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

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
        registerTypedCache(builder, baseMapper, PagedResponse.class, ProteinSummaryDto.class, "geneList", "geneSearch");
        registerTypedCache(builder, baseMapper, List.class, ReviewedRatioDto.class, "reviewedRatio", "filtered-reviewedRatio");
        registerTypedCache(builder, baseMapper, List.class, EvidenceDistributionDto.class, "evidenceLevels", "filtered-evidenceLevels");
        registerTypedCache(builder, baseMapper, List.class, KeywordFrequencyDto.class, "keywordFrequency", "filtered-keywordFrequency");
        registerTypedCache(builder, baseMapper, List.class, ProteinLengthWeightCount.class, "filtered-proteinLengthWeightCount");
        registerTypedCache(builder, baseMapper, List.class, String.class, "geneKeywords");

        return builder.build();
    }

    /**
     * Secondary CacheManager if you strictly need a fallback manager that handles
     * unconfigured caches containing Java Records or Non-Final objects.
     */
    @Bean
    public RedisCacheManager redisNonFinalAndRecordCacheManager(RedisConnectionFactory connectionFactory) {
        var nonFinalMapper = getBaseMapper(DefaultTyping.NON_FINAL_AND_RECORDS);
        var config = createBaseConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJacksonJsonRedisSerializer(nonFinalMapper)));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }

    private RedisCacheConfiguration createBaseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
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
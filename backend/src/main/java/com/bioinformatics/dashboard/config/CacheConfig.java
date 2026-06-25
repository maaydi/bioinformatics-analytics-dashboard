package com.bioinformatics.dashboard.config;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
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


    private static ObjectMapper getBaseObjectMapper() {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.bioinformatics.dashboard")
                .allowIfBaseType("java.util")
                .allowIfBaseType(java.lang.Object.class)
                .build();
        return JsonMapper.builder()
                .activateDefaultTyping(ptv, DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
                .build();
    }

    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var objectMapper = getBaseObjectMapper();
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(objectMapper)));
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public RedisCacheManager listOrganismCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();

        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, OrganismCountDto.class);

        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);

        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("byOrganism", cacheConfig)
                .withCacheConfiguration("filtered-byOrganism", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listLengthHistogramCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();

        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, LengthHistogramBucketDto.class);

        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);

        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("lengthHistogram", cacheConfig)
                .withCacheConfiguration("filtered-lengthHistogram", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager pageResponseSavedFiltersCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();

        var pageType = TypeFactory.createDefaultInstance()
                .constructParametricType(PagedResponse.class, SavedFilterDto.class);

        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, pageType);

        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("savedFilters", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listReviewedRatioCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();
        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, ReviewedRatioDto.class);
        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("reviewedRatio", cacheConfig)
                .withCacheConfiguration("filtered-reviewedRatio", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listEvidenceLevelsCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();
        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, EvidenceDistributionDto.class);
        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("evidenceLevels", cacheConfig)
                .withCacheConfiguration("filtered-evidenceLevels", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listKeywordFrequencyCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();
        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, KeywordFrequencyDto.class);
        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("keywordFrequency", cacheConfig)
                .withCacheConfiguration("filtered-keywordFrequency", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listProteinLengthWeightCountCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();
        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, ProteinLengthWeightCount.class);
        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("filtered-proteinLengthWeightCount", cacheConfig)
                .build();
    }

    @Bean
    public RedisCacheManager listStringCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var cleanMapper = getBaseObjectMapper();
        var listType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, String.class);
        var serializer = new JacksonJsonRedisSerializer<>(cleanMapper, listType);
        var cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("geneKeywords", cacheConfig)
                .build();
    }
}

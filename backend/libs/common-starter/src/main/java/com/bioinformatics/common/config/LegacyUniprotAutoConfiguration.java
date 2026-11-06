package com.bioinformatics.common.config;

import com.bioinformatics.common.providers.uniprotkb.config.RetryConfig;
import com.bioinformatics.common.providers.uniprotkb.config.UniprotRestClientConfig;
import com.bioinformatics.common.providers.uniprotkb.gene.specification.GeneSpecification;
import com.bioinformatics.common.providers.uniprotkb.mapper.UniProtProteinDtoMapper;
import com.bioinformatics.common.providers.uniprotkb.service.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration(afterName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnClass({org.springframework.web.client.RestClient.class,
        org.springframework.retry.annotation.EnableRetry.class, jakarta.persistence.Entity.class})
@ConditionalOnProperty(prefix = "common", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.legacy-catalog", name = "enabled", havingValue = "true")
@Import({UniprotRestClientConfig.class, RetryConfig.class, GeneSpecification.class, UniProtProteinDtoMapper.class,
        DatabaseRestService.class, SearchFieldRestService.class, SuggesterRestService.class,
        UniProtKbApiClient.class, UniProtSearchFieldService.class, UniprotKbRestService.class,
        UniprotKbPaginationCacheService.class})
public class LegacyUniprotAutoConfiguration {
}

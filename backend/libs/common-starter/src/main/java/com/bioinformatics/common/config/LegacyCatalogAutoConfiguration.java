package com.bioinformatics.common.config;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.mapper.GeneMapper;
import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.uniprot.mapper.UniProtEntryMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration(afterName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnClass(org.springframework.data.jpa.repository.JpaRepository.class)
@ConditionalOnProperty(prefix = "common", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.legacy-catalog", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CommonProperties.class)
@EntityScan(basePackageClasses = ProteinEntry.class)
@EnableJpaRepositories(basePackageClasses = ProteinEntryRepository.class)
@Import({ProteinEntryService.class, UniProtEntryMapper.class})
public class LegacyCatalogAutoConfiguration {

    @Bean
    public GeneMapper geneMapper() {
        return Mappers.getMapper(GeneMapper.class);
    }
}

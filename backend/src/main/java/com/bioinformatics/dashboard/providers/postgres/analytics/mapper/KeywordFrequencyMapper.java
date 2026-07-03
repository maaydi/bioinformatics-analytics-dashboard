package com.bioinformatics.dashboard.providers.postgres.analytics.mapper;

import com.bioinformatics.dashboard.model.analytics.KeywordFrequencyDto;
import com.bioinformatics.dashboard.providers.postgres.analytics.entity.KeywordFrequency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KeywordFrequencyMapper {

    KeywordFrequencyDto toDto(KeywordFrequency entity);


}

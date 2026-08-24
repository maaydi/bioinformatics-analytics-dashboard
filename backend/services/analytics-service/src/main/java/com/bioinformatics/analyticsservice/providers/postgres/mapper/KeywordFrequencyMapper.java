package com.bioinformatics.analyticsservice.providers.postgres.mapper;

import com.bioinformatics.analyticsservice.models.KeywordFrequencyDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.KeywordFrequency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KeywordFrequencyMapper {

    KeywordFrequencyDto toDto(KeywordFrequency entity);


}

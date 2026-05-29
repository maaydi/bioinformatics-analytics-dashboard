package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.dto.KeywordFrequencyDto;
import com.bioinformatics.dashboard.analytics.entity.KeywordFrequency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KeywordFrequencyMapper {

    KeywordFrequencyDto toDto(KeywordFrequency entity);


}

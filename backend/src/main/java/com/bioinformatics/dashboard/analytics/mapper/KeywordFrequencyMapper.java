package com.bioinformatics.dashboard.analytics.mapper;

import com.bioinformatics.dashboard.analytics.entity.KeywordFrequency;
import com.bioinformatics.dashboard.model.analytics.KeywordFrequencyDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KeywordFrequencyMapper {

    KeywordFrequencyDto toDto(KeywordFrequency entity);


}

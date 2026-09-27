package com.bioinformatics.exportservice.config;

import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.dto.converter.ExportFormatDeserializer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleDeserializers;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer customizeJackson() {
        return builder -> {
            var customDeserializers = new SimpleDeserializers();
            customDeserializers.addDeserializer(ExportFormat.class, new ExportFormatDeserializer());

            builder.deserializerFactory(
                    builder.deserializerFactory().withAdditionalDeserializers(customDeserializers)
            );
        };
    }
}

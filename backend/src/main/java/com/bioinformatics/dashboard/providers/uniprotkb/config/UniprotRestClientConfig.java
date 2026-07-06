package com.bioinformatics.dashboard.providers.uniprotkb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class UniprotRestClientConfig {

    @Bean
    public RestClient uniprotRestClient() {

        var nonFinalMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .build();
        var jacksonConverter =
                new JacksonJsonHttpMessageConverter(nonFinalMapper);
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofHours(1));


        return RestClient.builder().baseUrl("https://rest.uniprot.org/uniprotkb")
                .configureMessageConverters(builder -> {
                    builder.registerDefaults();
                    builder.withJsonConverter(jacksonConverter);
                })
                .defaultHeader("Connection", "close")
                .requestFactory(factory)
                .build();
    }
}

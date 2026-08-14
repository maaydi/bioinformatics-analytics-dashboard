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

/**
 * Configuration bean factory for a UniProt REST client.
 *
 * <p>Constructs a {@link RestClient} configured specifically for UniProt API consumption,
 * including:</p>
 * <ul>
 *   <li>Jackson deserialization configured to tolerate unknown properties and null primitives,
 *       reflecting the partial nature of UniProt API responses</li>
 *   <li>HTTP/1.1 client with a 1-hour read timeout to accommodate large API result sets</li>
 *   <li>Connection close header to avoid resource exhaustion on repeated calls</li>
 * </ul>
 *
 * <p>The 1-hour timeout is intentional for batch or analytical queries that may
 * process large protein datasets.</p>
 */
@Configuration
public class UniprotRestClientConfig {

    /**
     * Produces the UniProt REST client bean.
     *
     * @return a configured {@link RestClient} targeting the UniProt public API
     */
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


        return RestClient.builder().baseUrl("https://rest.uniprot.org")
                .configureMessageConverters(builder -> {
                    builder.registerDefaults();
                    builder.withJsonConverter(jacksonConverter);
                })
                .defaultHeader("Connection", "close")
                .requestFactory(factory)
                .build();
    }
}

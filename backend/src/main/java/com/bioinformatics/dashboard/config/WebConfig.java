package com.bioinformatics.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC / CORS configuration.
 *
 * <p>In production the Angular app is served by nginx on the same origin,
 * so CORS is only needed for local development (ng serve on :4200 → backend on :8080).
 * Restrict origins via {@code app.cors.allowed-origins} in application.yml before deploying.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}

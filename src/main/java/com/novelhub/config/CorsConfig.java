package com.novelhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration
 * Configures Cross-Origin Resource Sharing for the application
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Allow specific origins
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://*.blfly.com"
                )
                // Allow specific HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // Allow specific headers
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "fp",
                        "fp1",
                        "x-guide",
                        "X-code",
                        "theme-version"
                )
                // Allow credentials
                .allowCredentials(true)
                // Cache CORS configuration for 1 hour
                .maxAge(3600);
    }
}


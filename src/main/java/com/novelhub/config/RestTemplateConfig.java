package com.novelhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 * Provides a default RestTemplate bean for dependency injection
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create a default RestTemplate bean
     * Uses JDK's HttpURLConnection as the underlying HTTP client
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


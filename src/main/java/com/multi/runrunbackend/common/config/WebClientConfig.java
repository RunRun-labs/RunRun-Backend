package com.multi.runrunbackend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : WebClientConfig
 * @since : 2025. 12. 18. Thursday
 */
@Configuration
public class WebClientConfig {

    @Value("${tmap.base-url}")
    private String tmapBaseUrl;

    @Bean
    public WebClient tmapWebClient() {
        return WebClient.builder()
            .baseUrl(tmapBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}

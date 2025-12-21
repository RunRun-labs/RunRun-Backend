package com.multi.runrunbackend.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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

    @Bean(name = "tmapObjectMapper")
    public ObjectMapper tmapObjectMapper(JtsModule jtsModule) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(jtsModule);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        return mapper;
    }

    @Bean
    public WebClient tmapWebClient(@Qualifier("tmapObjectMapper") ObjectMapper tmapObjectMapper) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(tmapObjectMapper, MediaType.APPLICATION_JSON);
                Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(tmapObjectMapper, MediaType.APPLICATION_JSON);
                configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                configurer.defaultCodecs().jackson2JsonDecoder(decoder);
            })
            .build();

        return WebClient.builder()
            .baseUrl(tmapBaseUrl)
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}

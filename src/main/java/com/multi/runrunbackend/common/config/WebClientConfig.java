package com.multi.runrunbackend.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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

    @Value("${mapbox.timeout.connect-ms}")
    private int connectMs;

    @Value("${mapbox.timeout.response-ms}")
    private int responseMs;

    @Value("${mapbox.response.max-bytes}")
    private int maxBytes;

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
                    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(tmapObjectMapper,
                            MediaType.APPLICATION_JSON);
                    Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(tmapObjectMapper,
                            MediaType.APPLICATION_JSON);
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

    @Bean(name = "mapboxWebClient")
    public WebClient mapboxWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(responseMs));

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://api.mapbox.com");
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(maxBytes))
                .build();

        return WebClient.builder()
                .baseUrl("https://api.mapbox.com")
                .uriBuilderFactory(factory)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

    }
}

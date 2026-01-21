package com.multi.runrunbackend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @author : changwoo
 * @description : WebSocket 설정
 * @filename : WebSocketConfig
 * @since : 2025-12-17 수요일
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${WS_TEST_ENABLED:false}")
    private boolean wsTestEnabled;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");  // 구독 경로
        registry.setApplicationDestinationPrefixes("/pub");  // 발행 경로
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // WebSocket 연결 엔드포인트
            .setAllowedOriginPatterns("*")
            .withSockJS();
        if (wsTestEnabled) {
            registry.addEndpoint("/ws-test")
                .setAllowedOriginPatterns("*");
        }
    }


}
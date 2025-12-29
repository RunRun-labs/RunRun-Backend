package com.multi.runrunbackend.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author : changwoo
 * @description : RedisPublisher  ->    메시지를 Redis 채널에 발행
 * @filename : RedisPublisher
 * @since : 2025-12-17 수요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisPubSubTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 채팅 메시지 발행
   */
  public void publish(String channel, ChatMessageDto message) {
    try {
      // 객체를 JSON 문자열로 변환 (@class 필드 없음)
      String jsonMessage = objectMapper.writeValueAsString(message);
      redisPubSubTemplate.convertAndSend(channel, jsonMessage);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 직렬화 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * 제네릭 메시지 발행 (GPS 통계 등)
   */
  public void publishObject(String channel, Object message) {
    try {
      // 객체를 JSON 문자열로 변환 (@class 필드 없음)
      String jsonMessage = objectMapper.writeValueAsString(message);
      log.debug("📤 Redis Pub: channel={}", channel);
      redisPubSubTemplate.convertAndSend(channel, jsonMessage);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 직렬화 실패: {}", e.getMessage(), e);
    }
  }
}

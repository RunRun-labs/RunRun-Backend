package com.multi.runrunbackend.domain.chat.service;

import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author : changwoo
 * @description : RedisPublisher  ->    메시지를 Redis 채널에 발행
 * @filename : RedisPublisher
 * @since : 2025-12-17 수요일
 */
@Service
@RequiredArgsConstructor
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisPubSubTemplate;


  public void publish(String channel, ChatMessageDto message) {
    redisPubSubTemplate.convertAndSend(channel, message);
  }
}
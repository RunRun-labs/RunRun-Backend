package com.multi.runrunbackend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.multi.runrunbackend.domain.chat.service.RedisSubscriber;
import com.multi.runrunbackend.domain.running.service.RunningStatsSubscriber;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    return template;
  }

  /**
   * GPS 데이터 저장용 RedisTemplate
   * - String으로 저장 (JSON 문자열)
   */
  @Bean
  public RedisTemplate<String, String> gpsRedisTemplate(
      RedisConnectionFactory connectionFactory) {
    
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(RedisSerializer.string());
    
    // String 직렬화 사용 (JSON 문자열)
    template.setValueSerializer(RedisSerializer.string());
    
    return template;
  }


  /**
   * 채팅메시지 같은 객체를 Pub/Sub으로 전송
   * String으로 직렬화 (JSON 문자열) - @class 필드 없음
   */
  @Bean
  public RedisTemplate<String, Object> redisPubSubTemplate(
      RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(RedisSerializer.string());
    
    // String으로 직렬화 (가장 안전한 방법, deprecated 없음)
    template.setValueSerializer(RedisSerializer.string());
    
    return template;
  }

  /**
   * PatternTopic - Redis Pub/Sub에서 사용할 채널 패턴 정의
   * chat:* 패턴으로 모든 세션 채널 수신
   */
  @Bean
  public PatternTopic chatTopic() {
    return new PatternTopic("chat:*");  // 패턴 매칭
  }

  /**
   * GPS 통계 토픽 - running:* 패턴
   */
  @Bean
  public PatternTopic runningTopic() {
    return new PatternTopic("running:*");
  }

  /**
   * Redis Pub/Sub 메시지 리스너 컨테이너 - Redis메시지를 수신하는 컨테이너
   */
  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter chatListenerAdapter,
      MessageListenerAdapter runningListenerAdapter,
      PatternTopic chatTopic,
      PatternTopic runningTopic
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    
    // 채팅 메시지 리스너
    container.addMessageListener(chatListenerAdapter, chatTopic);
    
    // GPS 통계 리스너
    container.addMessageListener(runningListenerAdapter, runningTopic);
    
    return container;
  }

  /**
   * 채팅 메시지 리스너 어댑터
   */
  @Bean
  public MessageListenerAdapter chatListenerAdapter(RedisSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  /**
   * GPS 통계 리스너 어댑터
   */
  @Bean
  public MessageListenerAdapter runningListenerAdapter(RunningStatsSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "handleMessage");
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

}

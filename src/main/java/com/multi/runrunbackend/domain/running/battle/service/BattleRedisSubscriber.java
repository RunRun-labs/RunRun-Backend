package com.multi.runrunbackend.domain.running.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * @author : chang
 * @description : 배틀 Redis Pub/Sub Subscriber - 다중 서버 환경에서 WebSocket 메시지 브로드캐스트
 * @filename : BattleRedisSubscriber
 * @since : 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleRedisSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      // Redis에서 받은 메시지를 파싱
      String channel = new String(message.getChannel());
      String payload = new String(message.getBody());

      log.info("📨 [Redis Pub/Sub] 메시지 수신 - 채널: {}", channel);

      // JSON 파싱
      Map<String, Object> data = objectMapper.readValue(payload, Map.class);

      // destination 추출 (예: /sub/battle/1/ready)
      String destination = (String) data.get("destination");
      Object messageData = data.get("message");

      if (destination == null) {
        log.warn("⚠️ destination이 없는 메시지: {}", payload);
        return;
      }

      // WebSocket으로 브로드캐스트
      messagingTemplate.convertAndSend(destination, messageData);

      log.info("✅ [WebSocket] 메시지 전송 완료 - destination: {}", destination);

    } catch (Exception e) {
      log.error("❌ Redis 메시지 처리 실패", e);
    }
  }
}

package com.multi.runrunbackend.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * @author : changwoo
 * @description :  RedisSubscriber ->  Redis 채널에서 메시지수신 -> 웹소켓 전달
 * @filename : RedisSubscriber
 * @since : 2025-12-17 수요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final SimpMessageSendingOperations messagingTemplate;

  /**
   * ⭐ MessageListener 인터페이스 구현 - Redis 메시지 수신
   */
  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String channel = new String(message.getChannel());
      String publishMessage = new String(message.getBody());

      log.info("📩 Redis 메시지 수신: channel={}", channel);

      // 채널명으로 구분
      if (channel.startsWith("crew-chat:")) {
        sendCrewChatMessage(publishMessage);
      } else if (channel.startsWith("chat:")) {
        sendOfflineChatMessage(publishMessage);
      } else {
        log.warn("⚠️ 알 수 없는 채널: {}", channel);
      }
    } catch (Exception e) {
      log.error("❌ 메시지 처리 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * 오프라인 채팅 메시지 처리
   */
  private void sendOfflineChatMessage(String publishMessage) throws Exception {
    ChatMessageDto message = objectMapper.readValue(publishMessage, ChatMessageDto.class);

    // 해당 세션 구독자들에게 WebSocket으로 전송
    messagingTemplate.convertAndSend(
        "/sub/chat/" + message.getSessionId(),
        message
    );
    log.info("✅ 오프라인 채팅 메시지 WebSocket 전송: sessionId={}", message.getSessionId());
  }

  /**
   * 크루 채팅 메시지 처리
   */
  private void sendCrewChatMessage(String publishMessage) throws Exception {
    CrewChatMessageDto message = objectMapper.readValue(publishMessage, CrewChatMessageDto.class);

    // 해당 채팅방 구독자들에게 WebSocket으로 전송
    messagingTemplate.convertAndSend(
        "/sub/crew-chat/" + message.getRoomId(),
        message
    );
    log.info("✅ 크루 채팅 메시지 WebSocket 전송: roomId={}, sender={}, content={}", 
        message.getRoomId(), message.getSenderName(), message.getContent());
  }

}

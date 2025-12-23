package com.multi.runrunbackend.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class RedisSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessageSendingOperations messagingTemplate;

  public void sendMessage(String publishMessage) {
    try {
      ChatMessageDto message = objectMapper.readValue(publishMessage, ChatMessageDto.class);

      // 해당 세션 구독자들에게 WebSocket으로 전송
      messagingTemplate.convertAndSend(
          "/sub/chat/" + message.getSessionId(),
          message
      );
    } catch (Exception e) {
      log.error("메시지 처리 실패: {}", e.getMessage());
    }
  }

}

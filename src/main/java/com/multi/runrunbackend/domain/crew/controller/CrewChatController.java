package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.domain.crew.dto.req.CrewChatMessageDto;
import com.multi.runrunbackend.domain.crew.service.CrewChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * @author : changwoo
 * @description : 크루 채팅 WebSocket 컨트롤러
 * @filename : CrewChatController
 * @since : 2026-01-04
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CrewChatController {

  private final CrewChatService crewChatService;

  /**
   * 크루 채팅 메시지 전송
   */
  @MessageMapping("/crew-chat/message")
  public void sendMessage(@Payload CrewChatMessageDto message) {
    log.info("크루 메시지: {} - {}", message.getSenderName(), message.getContent());
    crewChatService.sendMessage(message);
  }

  // - 가입 승인 시: CrewChatService.addUserToChatRoom() → sendJoinMessage() 자동 호출
  // - 크루 탈퇴 시: CrewChatService.removeUserFromChatRoom() → sendLeaveMessage() 자동 호출

}

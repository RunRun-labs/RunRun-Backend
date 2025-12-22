package com.multi.runrunbackend.domain.chat.controller;

import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import com.multi.runrunbackend.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : ChatController
 * @since : 2025-12-17 수요일
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final ChatService chatService;
  
  @MessageMapping("/chat/message")
  public void sendMessage(@Payload ChatMessageDto message) {
    log.info("메시지: {} - {}", message.getSenderName(), message.getContent());
    chatService.sendMessage(message);
  }


  @MessageMapping("/chat/enter")
  public void enterChatRoom(@Payload ChatMessageDto message) {
    log.info("입장: {} - 세션 {}", message.getSenderName(), message.getSessionId());
    chatService.sendEnterMessage(message.getSessionId(), message.getSenderName());
  }


  @MessageMapping("/chat/leave")
  public void leaveChatRoom(@Payload ChatMessageDto message) {
    log.info("퇴장: {} - 세션 {}", message.getSenderName(), message.getSessionId());
    chatService.sendLeaveMessage(message.getSessionId(), message.getSenderName());
  }

}

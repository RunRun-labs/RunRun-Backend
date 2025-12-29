package com.multi.runrunbackend.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : ChatMessageDto
 * @since : 2025-12-17 수요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageDto {

  private Long sessionId;
  private Long senderId;
  private String senderName;
  private String content;
  private String messageType;

  private LocalDateTime createdAt;
}

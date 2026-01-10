package com.multi.runrunbackend.domain.crew.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : changwoo
 * @description : 크루 채팅 메시지 DTO
 * @filename : CrewChatMessageDto
 * @since : 2026-01-04
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrewChatMessageDto {

  private Long roomId;
  private Long senderId;
  private String senderName;
  private String content;
  private String messageType;
  private LocalDateTime createdAt;

}

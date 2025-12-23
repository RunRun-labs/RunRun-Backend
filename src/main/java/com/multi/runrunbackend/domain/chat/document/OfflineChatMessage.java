package com.multi.runrunbackend.domain.chat.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : OfflineChatMessage
 * @since : 2025-12-18 목요일
 */
@Document(collection = "offline_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineChatMessage {

  @Id
  private String id;

  @Field("session_id")
  private Long sessionId;

  @Field("sender_id")
  private Long senderId;

  @Field("sender_name")
  private String senderName;

  @Field("content")
  private String content;

  @Field("message_type")
  private String messageType;

  @CreatedDate
  @Field("created_at")
  private LocalDateTime createdAt;


}

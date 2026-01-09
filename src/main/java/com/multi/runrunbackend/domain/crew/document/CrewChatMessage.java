package com.multi.runrunbackend.domain.crew.document;

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
 * @description : 크루 채팅 메시지 (MongoDB)
 * @filename : CrewChatMessage
 * @since : 2026-01-04
 */
@Document(collection = "crew_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewChatMessage {

  @Id
  private String id;

  @Field("room_id")
  private Long roomId;

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

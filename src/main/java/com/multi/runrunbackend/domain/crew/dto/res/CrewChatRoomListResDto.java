package com.multi.runrunbackend.domain.crew.dto.res;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : changwoo
 * @description : 크루 채팅방 목록 응답 DTO
 * @filename : CrewChatRoomListResDto
 * @since : 2026-01-04
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewChatRoomListResDto {

  // 채팅방 기본 정보
  private Long roomId;
  private String roomName;          // 채팅방 이름

  // 크루 정보
  private Long crewId;
  private String crewName;          // 크루 이름
  private String crewDescription;   // 크루 설명

  // 참가자 정보
  private Integer currentMembers;   // 현재 크루원 수

  // 최근 메시지 정보
  private String lastMessageContent;   // 최근 메시지 내용
  private String lastMessageSender;    // 최근 메시지 보낸 사람
  private LocalDateTime lastMessageTime; // 최근 메시지 시간

  // 읽지 않은 메시지
  private Integer unreadCount;

}

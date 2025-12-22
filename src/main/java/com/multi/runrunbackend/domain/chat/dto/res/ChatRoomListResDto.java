package com.multi.runrunbackend.domain.chat.dto.res;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListResDto {

  // 세션 기본 정보
  private Long sessionId;
  private String title;               // 모집글 제목
  private String meetingPlace;        // 만남 장소
  private LocalDateTime meetingAt;    // 만남 시간

  // 러닝 정보
  private Double targetDistance;      // 목표 거리 (km)
  private String targetPace;          // 목표 페이스
  private String formattedDuration;   // 소요시간 ("2시간 30분")

  // 참가자 정보
  private Integer currentParticipants; // 현재 참가자 수
  private Integer maxParticipants;     // 최대 참가자 수
  private Integer readyCount;          // 준비 완료 수

  // 세션 상태
  private String sessionStatus;        // WAITING, READY, IN_PROGRESS, COMPLETED

  // 최근 메시지 정보
  private String lastMessageContent;   // 최근 메시지 내용
  private String lastMessageSender;    // 최근 메시지 보낸 사람
  private LocalDateTime lastMessageTime; // 최근 메시지 시간

  // 읽지 않은 메시지 (추후 구현 가능)
  private Integer unreadCount;


}

package com.multi.runrunbackend.domain.chat.dto.res;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : changwoo
 * @description : 통합 채팅방 목록 응답 DTO (오프라인 + 크루)
 * @filename : UnifiedChatRoomResDto
 * @since : 2026-01-04
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatRoomResDto {

  // 공통 - 채팅방 구분
  private String chatType;            // "OFFLINE" or "CREW"
  
  // 공통 - 채팅방 식별
  private Long chatRoomId;            // sessionId or roomId
  private String chatRoomTitle;       // 제목

  // 공통 - 참가자 정보
  private Integer currentParticipants;
  
  // 공통 - 최근 메시지 정보
  private String lastMessageContent;
  private String lastMessageSender;
  private LocalDateTime lastMessageTime;
  private Integer unreadCount;

  // 오프라인 전용
  private String meetingPlace;
  private LocalDateTime meetingAt;
  private Double targetDistance;
  private String targetPace;
  private String formattedDuration;
  private Integer maxParticipants;
  private Integer readyCount;
  private String sessionStatus;

  // 크루 전용
  private Long crewId;
  private String crewName;
  private String crewDescription;
  private String crewImageUrl;

  /**
   * 오프라인 채팅방 → 통합 DTO 변환
   */
  public static UnifiedChatRoomResDto fromOffline(ChatRoomListResDto offline) {
    return UnifiedChatRoomResDto.builder()
        .chatType("OFFLINE")
        .chatRoomId(offline.getSessionId())
        .chatRoomTitle(offline.getTitle())
        .currentParticipants(offline.getCurrentParticipants())
        .lastMessageContent(offline.getLastMessageContent())
        .lastMessageSender(offline.getLastMessageSender())
        .lastMessageTime(offline.getLastMessageTime())
        .unreadCount(offline.getUnreadCount())
        // 오프라인 전용 필드
        .meetingPlace(offline.getMeetingPlace())
        .meetingAt(offline.getMeetingAt())
        .targetDistance(offline.getTargetDistance())
        .targetPace(offline.getTargetPace())
        .formattedDuration(offline.getFormattedDuration())
        .maxParticipants(offline.getMaxParticipants())
        .readyCount(offline.getReadyCount())
        .sessionStatus(offline.getSessionStatus())
        .build();
  }

  /**
   * 크루 채팅방 → 통합 DTO 변환
   */
  public static UnifiedChatRoomResDto fromCrew(
      com.multi.runrunbackend.domain.crew.dto.res.CrewChatRoomListResDto crew) {
    return UnifiedChatRoomResDto.builder()
        .chatType("CREW")
        .chatRoomId(crew.getRoomId())
        .chatRoomTitle(crew.getRoomName())
        .currentParticipants(crew.getCurrentMembers())
        .lastMessageContent(crew.getLastMessageContent())
        .lastMessageSender(crew.getLastMessageSender())
        .lastMessageTime(crew.getLastMessageTime())
        .unreadCount(crew.getUnreadCount())
        // 크루 전용 필드
        .crewId(crew.getCrewId())
        .crewName(crew.getCrewName())
        .crewDescription(crew.getCrewDescription())
        .crewImageUrl(crew.getCrewImageUrl())
        .build();
  }

}

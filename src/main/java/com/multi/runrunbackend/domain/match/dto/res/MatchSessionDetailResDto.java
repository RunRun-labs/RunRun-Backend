package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 세션 상세 정보 응답 DTO
 * @filename : MatchSessionDetailResDto
 * @since : 2025-12-29
 */
@Getter
@Builder
public class MatchSessionDetailResDto {

  private Long sessionId;
  private Long hostId;
  private Long courseId;
  private String courseName;
  private String courseImageUrl;
  private Double targetDistance;
  private String status;
  private Double startLat;
  private Double startLng;
  private String meetingPlace;
  private List<ParticipantInfo> participants;

  @Getter
  @Builder
  public static class ParticipantInfo {
    private Long userId;
    private String loginId;
    private String name;
    private boolean isReady;
  }

  public static MatchSessionDetailResDto from(MatchSession session, List<SessionUser> sessionUsers) {
    List<ParticipantInfo> participants = sessionUsers.stream()
        .map(su -> ParticipantInfo.builder()
            .userId(su.getUser().getId())
            .loginId(su.getUser().getLoginId())
            .name(su.getUser().getName())
            .isReady(su.isReady())
            .build())
        .collect(Collectors.toList());

    return MatchSessionDetailResDto.builder()
        .sessionId(session.getId())
        .hostId(session.getRecruit() != null ? session.getRecruit().getUser().getId() : null)
        .courseId(session.getCourse() != null ? session.getCourse().getId() : null)
        .courseName(session.getCourse() != null ? session.getCourse().getTitle() : null)
        .courseImageUrl(session.getCourse() != null ? session.getCourse().getImageUrl() : null)
        .targetDistance(session.getTargetDistance())
        .status(session.getStatus().name())
        .startLat(session.getRecruit() != null ? session.getRecruit().getLatitude() : null)
        .startLng(session.getRecruit() != null ? session.getRecruit().getLongitude() : null)
        .meetingPlace(session.getRecruit() != null ? session.getRecruit().getMeetingPlace() : null)
        .participants(participants)
        .build();
  }
}

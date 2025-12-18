package com.multi.runrunbackend.domain.recruit.dto.res;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 모집글 조회 응답 dto
 * @filename : RecruitListResDto
 * @since : 2025-12-18 목요일
 */
@Getter
@Builder
public class RecruitListResDto {

  private Long recruitId;
  private String title;
  private String meetingPlace;
  private LocalDateTime meetingAt;
  private Double targetDistance;
  private String targetPace;
  private Integer maxParticipants;
  // private Integer currentParticipants; // 추후 recruit_users 테이블 카운트 연동 필요
  private String authorName;
  private String genderLimit;
  private Double distanceKm;  // 내 위치로부터의 거리 (계산값)

  public static RecruitListResDto from(Recruit recruit, Double distance) {
    return RecruitListResDto.builder()
        .recruitId(recruit.getId())
        .title(recruit.getTitle())
        .meetingPlace(recruit.getMeetingPlace())
        .meetingAt(recruit.getMeetingAt())
        .targetDistance(recruit.getTargetDistance())
        .targetPace(recruit.getTargetPace())
        .maxParticipants(recruit.getMaxParticipants())
        .authorName(recruit.getUser().getName())
        .genderLimit(recruit.getGenderLimit().name())
        .distanceKm(distance != null ? Math.round(distance * 100) / 100.0 : null)
        .build();
  }
}

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
  private Integer currentParticipants;
  private String authorName;
  private String genderLimit;
  private Double distanceKm;
  private Integer ageMin;
  private Integer ageMax;
  private Double latitude;
  private Double longitude;
  private Boolean isAuthor;

  public static RecruitListResDto from(Recruit recruit, Double distance, Long currentUserId) {
    boolean isAuthor = currentUserId != null && recruit.getUser().getId().equals(currentUserId);
    
    return RecruitListResDto.builder()
        .recruitId(recruit.getId())
        .title(recruit.getTitle())
        .meetingPlace(recruit.getMeetingPlace())
        .meetingAt(recruit.getMeetingAt())
        .targetDistance(recruit.getTargetDistance())
        .targetPace(recruit.getTargetPace())
        .maxParticipants(recruit.getMaxParticipants())
        .currentParticipants(recruit.getCurrentParticipants())
        .authorName(recruit.getUser().getName())
        .genderLimit(recruit.getGenderLimit().name())
        .distanceKm(distance != null ? Math.round(distance * 100) / 100.0 : null)
        .ageMin(recruit.getAgeMin())
        .ageMax(recruit.getAgeMax())
        .latitude(recruit.getLatitude())
        .longitude(recruit.getLongitude())
        .isAuthor(isAuthor)
        .build();
  }
}

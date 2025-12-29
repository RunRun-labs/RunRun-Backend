package com.multi.runrunbackend.domain.recruit.dto.res;

import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RecruitDetailResDto
 * @since : 2025-12-18 목요일
 */
@Getter
@Builder
public class RecruitDetailResDto {

  private Long recruitId;
  private String title;
  private String content;
  private String meetingPlace;
  private Double latitude;
  private Double longitude;
  private LocalDateTime meetingAt;
  private Double targetDistance;
  private String targetPace;
  private Integer maxParticipants;
  private Integer currentParticipants;

  private String authorName;
  private Long authorId;

  private String genderLimit;
  private LocalDateTime createdAt;

  private Long courseId;
  private String courseImageUrl;
  private Integer ageMin;
  private Integer ageMax;

  private Boolean isAuthor;
  private Boolean isParticipant;
  private RecruitStatus status;

  public static RecruitDetailResDto from(Recruit recruit, Long currentUserId,
      boolean isParticipant) {
    boolean isAuthor = currentUserId != null && recruit.getUser().getId().equals(currentUserId);

    return RecruitDetailResDto.builder()
        .recruitId(recruit.getId())
        .title(recruit.getTitle())
        .content(recruit.getContent())
        .meetingPlace(recruit.getMeetingPlace())
        .latitude(recruit.getLatitude())
        .longitude(recruit.getLongitude())
        .meetingAt(recruit.getMeetingAt())
        .targetDistance(recruit.getTargetDistance())
        .targetPace(recruit.getTargetPace())
        .maxParticipants(recruit.getMaxParticipants())
        .currentParticipants(recruit.getCurrentParticipants())
        .authorName(recruit.getUser().getName())
        .authorId(recruit.getUser().getId())
        .genderLimit(recruit.getGenderLimit().name())
        .createdAt(recruit.getCreatedAt())
        .courseId(recruit.getCourse() != null ? recruit.getCourse().getId() : null)
        .courseImageUrl(recruit.getCourse() != null ? recruit.getCourse().getImageUrl() : null)
        .ageMin(recruit.getAgeMin())
        .ageMax(recruit.getAgeMax())
        .isAuthor(isAuthor)
        .isParticipant(isParticipant)
        .status(recruit.getStatus())
        .build();
  }
}
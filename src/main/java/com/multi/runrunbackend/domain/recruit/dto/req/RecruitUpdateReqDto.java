package com.multi.runrunbackend.domain.recruit.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : KIMGWANGHO
 * @description : *수정하기* 응답 클래스
 * @filename : RecruitUpdateReqDto
 * @since : 2025-12-18 목요일
 */
@Getter
@Setter
public class RecruitUpdateReqDto {

  @NotBlank(message = "제목은 필수입니다.")
  private String title;

  @NotBlank(message = "내용은 필수입니다.")
  private String content;

  @NotNull(message = "모임 시간은 필수입니다.")
  private LocalDateTime meetingAt;

  private String meetingPlace;
  private Double latitude;
  private Double longitude;
  private Double targetDistance;
  private String targetPace;
  private Integer maxParticipants;
  private GenderLimit genderLimit;
  private Long courseId; // 코스 변경 시 사용

}
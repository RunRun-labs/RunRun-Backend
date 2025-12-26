package com.multi.runrunbackend.domain.recruit.dto.req;

import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.recruit.constant.GenderLimit;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : 클라이언트로부터 러닝 모집글 생성 요청 데이터를 전달받고 유효성을 검증하는 요청 DTO 클래스
 * @filename : RecruitCreateRequest
 * @since : 2025-12-17 수요일
 */
@Getter
@NoArgsConstructor
public class RecruitCreateReqDto {

  @NotBlank(message = "제목은 필수입니다.")
  @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
  private String title;

  @NotBlank(message = "내용은 필수입니다.")
  @Size(max = 500, message = "내용은 500자 이내여야 합니다")
  private String content;

  @NotBlank(message = "모임 장소는 필수입니다.")
  private String meetingPlace;

  @NotNull(message = "위도는 필수입니다.")
  @DecimalMin(value = "-90.0", inclusive = true)
  @DecimalMax(value = "90.0", inclusive = true)
  private Double latitude;

  @NotNull(message = "경도는 필수입니다.")
  @DecimalMin(value = "-180.0", inclusive = true)
  @DecimalMax(value = "180.0", inclusive = true)
  private Double longitude;

  @NotNull(message = "뛸 거리는 필수입니다.")
  private Double targetDistance;

  @NotBlank(message = "목표 페이스는 필수입니다.")
  @Pattern(regexp = "^\\d{1,2}:\\d{2}$", message = "페이스는 '분:초' 형식이어야 합니다. (예: 6:00)")
  private String targetPace;

  @NotNull(message = "최대 인원은 필수입니다.")
  @Min(value = 2, message = "모집 인원은 최소 2명 이상이어야 합니다.")
  @Max(value = 20, message = "최대 인원은 20명을 초과할 수 없습니다.")
  private Integer maxParticipants;

  @NotNull(message = "최소 나이는 필수입니다.")
  @Min(0)
  @Max(100)
  private Integer ageMin;

  @NotNull(message = "최대 나이는 필수입니다.")
  @Min(0)
  @Max(100)
  private Integer ageMax;

  @NotNull(message = "성별 제한은 필수입니다.")
  private GenderLimit genderLimit;

  @NotNull(message = "모임 시간은 필수입니다.")
  @Future(message = "모임 시간은 현재 시간 이후여야 합니다.")
  private LocalDateTime meetingAt;

  private Long courseId;

  public void validate() {
    if (this.ageMin > this.ageMax) {
      throw new ValidationException(ErrorCode.INVALID_AGE_RANGE);
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneHourLater = now.plusHours(1);

    if (this.meetingAt.isBefore(oneHourLater)) {
      throw new ValidationException(ErrorCode.INVALID_MEETING_TIME);
    }

    LocalDateTime oneWeekLater = now.plusWeeks(2);
    if (this.meetingAt.isAfter(oneWeekLater)) {
      throw new ValidationException(ErrorCode.INVALID_MEETING_TIME);
    }
  }

  public Recruit toEntity(User user, Course course) {
    return Recruit.builder()
        .user(user)
        .course(course)
        .title(this.title)
        .content(this.content)
        .meetingPlace(this.meetingPlace)
        .latitude(this.latitude)
        .longitude(this.longitude)
        .targetDistance(this.targetDistance)
        .targetPace(this.targetPace)
        .maxParticipants(this.maxParticipants)
        .ageMin(this.ageMin)
        .ageMax(this.ageMax)
        .genderLimit(this.genderLimit)
        .meetingAt(this.meetingAt)
        .build();
  }
}


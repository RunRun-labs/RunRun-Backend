package com.multi.runrunbackend.domain.recruit.dto.req;

import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.recruit.constant.GenderLimit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
  @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
  private String title;

  @NotBlank(message = "내용은 필수입니다.")
  @Size(max = 500, message = "내용은 500자 이내여야 합니다")
  private String content;

  @NotNull(message = "모임 시간은 필수입니다.")
  private LocalDateTime meetingAt;

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

  @NotNull(message = "성별 제한은 필수입니다.")
  private GenderLimit genderLimit;

  @NotNull(message = "최소 나이는 필수입니다.")
  @Min(0)
  @Max(100)
  private Integer ageMin;

  @NotNull(message = "최대 나이는 필수입니다.")
  @Min(0)
  @Max(100)
  private Integer ageMax;

  private Long courseId; // 코스 변경 시 사용

  public void validate() {
    if (this.ageMin > this.ageMax) {
      throw new ValidationException(ErrorCode.INVALID_AGE_RANGE);
    }

    LocalDateTime oneWeekLater = LocalDateTime.now().plusWeeks(2);
    if (this.meetingAt.isAfter(oneWeekLater)) {
      throw new ValidationException(ErrorCode.INVALID_MEETING_TIME);
    }
  }


}
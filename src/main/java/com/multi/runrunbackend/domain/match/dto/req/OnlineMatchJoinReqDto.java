package com.multi.runrunbackend.domain.match.dto.req;

import com.multi.runrunbackend.common.constant.DistanceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : OnlineMatchJoinReqDto
 * @since : 2025-12-27 토요일
 */

@Getter
@NoArgsConstructor
public class OnlineMatchJoinReqDto {

  @NotNull(message = "목표 거리는 필수입니다.")
  private DistanceType distance;

  @Min(value = 2, message = "최소 인원은 2명입니다.")
  @Max(value = 4, message = "최대 인원은 4명입니다.")
  private int participantCount;
}

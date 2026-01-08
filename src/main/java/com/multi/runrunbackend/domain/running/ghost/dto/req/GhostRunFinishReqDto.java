package com.multi.runrunbackend.domain.running.ghost.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 고스트런 완료 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GhostRunFinishReqDto {

  private Long userId;         // 사용자 ID
  private Double totalDistance;    // km
  private Integer totalTime;       // 초
  private Double avgPace;          // 분/km
}

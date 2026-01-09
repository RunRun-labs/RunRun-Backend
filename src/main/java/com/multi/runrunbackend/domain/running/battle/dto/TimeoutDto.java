package com.multi.runrunbackend.domain.running.battle.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : chang
 * @description : 배틀 타임아웃 정보
 * @filename : TimeoutData
 * @since : 2026-01-07
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeoutDto {

  /**
   * 첫 번째 완주자의 완주 시간
   */
  private LocalDateTime firstFinishTime;

  /**
   * 타임아웃 시간 (초)
   */
  private Integer timeoutSeconds;

  /**
   * 타이머 시작 여부
   */
  private Boolean isTimerStarted;
}

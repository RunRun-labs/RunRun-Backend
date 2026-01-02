package com.multi.runrunbackend.domain.match.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RunningResultFIlterType
 * @since : 2025-12-30 화요일
 */

@Getter
@RequiredArgsConstructor
public enum RunningResultFilterType {
  ALL("전체"),
  UNDER_3("3km 이하"),
  BETWEEN_3_5("3km ~ 5km"),
  BETWEEN_5_10("5km ~ 10km"),
  OVER_10("10km 이상");

  private final String description;
}

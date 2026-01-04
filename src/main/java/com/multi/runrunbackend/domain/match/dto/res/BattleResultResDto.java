package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : BattleResultResDto
 * @since : 2026-01-02 금요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultResDto {

  private Long battleResultId;
  private Long sessionId;

  private DistanceType distanceType;
  private LocalDateTime createdAt;

  private Integer ranking;
  private Integer totalTime;
  private BigDecimal avgPace;
  private BigDecimal totalDistance;

  private Integer previousRating;
  private Integer currentRating;
  private int delta;

  private SessionType sessionType;
  private SessionStatus sessionStatus;

  private long participants;


}

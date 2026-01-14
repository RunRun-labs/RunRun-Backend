package com.multi.runrunbackend.domain.running.battle.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : Redis에 저장할 배틀 참가자 실시간 데이터
 * @filename : BattleUserData
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUserDto implements Serializable {

  // private static final long serialVersionUID = 1L;
  //Java 직렬화

  private Long userId;
  private String username;
  private Double totalDistance;        // 총 주행 거리 (미터)
  private Double currentSpeed;         // 현재 속도 (m/s)
  private String currentPace;          // 현재 페이스 (분:초/km)
  private Double lastGpsLat;           // 마지막 GPS 위도
  private Double lastGpsLng;           // 마지막 GPS 경도
  private LocalDateTime lastGpsTime;   // 마지막 GPS 수신 시각
  private LocalDateTime startTime;     // 배틀 시작 시각
  private Boolean isFinished;          // 완주 여부
  private LocalDateTime finishTime;    // 완주 시각
  private String status;               // 상태: RUNNING, FINISHED, TIMEOUT, GIVE_UP
}

package com.multi.runrunbackend.domain.running.battle.dto.res;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : 배틀 참가자별 순위 정보 DTO
 * @filename : BattleRankingDto
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleRankingResDto {

  private Integer rank;                // 순위
  private Long userId;                 // 사용자 ID
  private String username;             // 사용자 이름
  private String profileImage;         // 프로필 이미지
  private Double totalDistance;        // 총 주행 거리 (미터)
  private Double remainingDistance;    // 남은 거리 (미터)
  private Double progressPercent;      // 진행률 (%)
  private String currentPace;          // 현재 페이스 (분:초/km)
  private Boolean isFinished;          // 완주 여부
  private Long finishTime;             // 완주 시간 (밀리초) - 경과 시간
  private LocalDateTime finishTimeActual; // ✅ 실제 완주 시각 (순위 비교용)
}

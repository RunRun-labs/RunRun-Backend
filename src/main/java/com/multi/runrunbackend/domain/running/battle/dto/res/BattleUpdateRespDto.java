package com.multi.runrunbackend.domain.running.battle.dto.res;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : 배틀 실시간 업데이트 응답 DTO (WebSocket 브로드캐스트용)
 * @filename : BattleUpdateResponse
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUpdateRespDto {

  private String type;                         // 메시지 타입 (BATTLE_UPDATE, BATTLE_START, BATTLE_FINISH 등)
  private Long sessionId;                      // 세션 ID
  private List<BattleRankingResDto> rankings;     // 전체 순위 정보
  private LocalDateTime timestamp;             // 전송 시각
}

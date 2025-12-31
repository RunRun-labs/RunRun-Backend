package com.multi.runrunbackend.domain.running.battle.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : 배틀 Ready 상태 변경 요청 DTO
 * @filename : BattleReadyRequest
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleReadyReqDto {

  private Long sessionId;
  private Long userId;  // 추가
  private Boolean isReady;
}

package com.multi.runrunbackend.domain.running.battle.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.running.battle.dto.res.BattleRankingResDto;
import com.multi.runrunbackend.domain.running.battle.service.BattleService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : chang
 * @description : 배틀 REST API 컨트롤러
 * @filename : BattleController
 * @since : 2025-12-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/battle")
@Slf4j
public class BattleController {

  private final BattleService battleService;

  /**
   * 현재 순위 조회
   */
  @GetMapping("/{sessionId}/rankings")
  public ResponseEntity<ApiResponse<List<BattleRankingResDto>>> getRankings(
      @PathVariable Long sessionId
  ) {
    log.info("📊 순위 조회 API: sessionId={}", sessionId);

    List<BattleRankingResDto> rankings = battleService.getRankings(sessionId);

    log.info("✅ 순위 조회 성공: sessionId={}, 참가자={}명", sessionId, rankings.size());

    return ResponseEntity.ok(ApiResponse.success(rankings));
  }

  /**
   * 배틀 결과 조회
   */
  @GetMapping("/{sessionId}/result")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getResult(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    Long userId = principal.getUserId();

    log.info("🏆 배틀 결과 조회: sessionId={}, userId={}", sessionId, userId);

    // 결과 조회
    Map<String, Object> result = battleService.getBattleResult(sessionId, userId);

    // ✅ Redis 삭제 안 함! (TTL로 자동 만료)
    // battleService.cleanupBattleDataNow(sessionId);  // 주석 처리

    log.info("✅ 결과 조회 성공: sessionId={}, userId={}, rank={}",
        sessionId, userId, result.get("myRank"));

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * 참가자 완주 처리
   */
  @PostMapping("/{sessionId}/finish")
  public ResponseEntity<ApiResponse<Void>> finishUser(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    Long userId = principal.getUserId();

    log.info("🏁 참가자 완주: sessionId={}, userId={}", sessionId, userId);

    battleService.finishUserAndCheckComplete(sessionId, userId);

    log.info("✅ 완주 처리 성공: sessionId={}, userId={}", sessionId, userId);

    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /**
   * 참가자 포기 처리
   */
  @PostMapping("/{sessionId}/quit")
  public ResponseEntity<ApiResponse<Void>> quitBattle(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    Long userId = principal.getUserId();

    log.info("🚨 참가자 포기: sessionId={}, userId={}", sessionId, userId);

    battleService.quitBattle(sessionId, userId);

    log.info("✅ 포기 처리 성공: sessionId={}, userId={}", sessionId, userId);

    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

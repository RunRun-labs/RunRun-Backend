package com.multi.runrunbackend.domain.running.battle.controller;

import com.multi.runrunbackend.common.exception.custom.CustomException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.running.battle.dto.req.BattleGpsReqDto;
import com.multi.runrunbackend.domain.running.battle.dto.req.BattleReadyReqDto;
import com.multi.runrunbackend.domain.running.battle.dto.res.BattleRankingResDto;
import com.multi.runrunbackend.domain.running.battle.service.BattleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * @author : chang
 * @description : 온라인 배틀 WebSocket 컨트롤러
 * @filename : BattleWebSocketController
 * @since : 2025-12-29
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BattleWebSocketController {

  private final BattleService battleService;
  // SimpMessagingTemplate 제거 - Redis Pub/Sub 사용

  @MessageMapping("/battle/ready")
  public void handleReady(BattleReadyReqDto request) {
    log.info("🎯 Ready 상태 변경 요청: sessionId={}, userId={}, isReady={}",
        request.getSessionId(), request.getUserId(), request.getIsReady());

    try {
      Long userId = request.getUserId();

      // toggleReady 반환값 사용 (allReady)
      boolean allReady = battleService.toggleReady(request.getSessionId(), userId,
          request.getIsReady());

      // Redis Pub/Sub으로 Ready 메시지 전송
      battleService.sendReadyMessage(
          request.getSessionId(),
          userId,
          request.getIsReady(),
          allReady
      );

      log.info("✅ Ready 상태 브로드캐스트: sessionId={}, userId={}, isReady={}, allReady={}",
          request.getSessionId(), userId, request.getIsReady(), allReady);

      // 모두 Ready면 자동 시작
      if (allReady) {
        log.info("🎉 모두 Ready! 자동 시작: sessionId={}", request.getSessionId());

        // 1초 대기 (UI 업데이트 시간)
        Thread.sleep(1000);

        // 배틀 시작 (Service에서 Redis Pub/Sub 메시지 전송)
        battleService.startBattle(request.getSessionId());

        log.info("🚩 배틀 시작 브로드캐스트: sessionId={}", request.getSessionId());

        // 초기 순위 전송 (0m로 초기화된 상태)
        List<BattleRankingResDto> initialRankings = battleService.getRankings(
            request.getSessionId());
        
        battleService.sendRankingMessage(request.getSessionId(), initialRankings);

        log.info("📊 초기 순위 전송: sessionId={}, 참가자={}명",
            request.getSessionId(), initialRankings.size());
      }

    } catch (InterruptedException e) {
      log.error("❌ Thread sleep 실패: sessionId={}", request.getSessionId(), e);
      Thread.currentThread().interrupt();

    } catch (CustomException e) {
      // ValidationException, NotFoundException 등 모든 커스텀 Exception 처리
      log.error("❌ Ready 처리 실패 - {}: sessionId={}", 
          e.getErrorCode().getMessage(), request.getSessionId());
      battleService.sendErrorMessage(request.getSessionId(), e.getErrorCode());

    } catch (Exception e) {
      log.error("❌ Ready 처리 실패: sessionId={}", request.getSessionId(), e);
      battleService.sendErrorMessage(request.getSessionId(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }


  @MessageMapping("/battle/gps")
  public void handleGps(BattleGpsReqDto request) {
    try {
      Long userId = request.getUserId();
      Double totalDistance = request.getTotalDistance();

      log.info("📍 GPS 수신: sessionId={}, userId={}, distance={}m",
          request.getSessionId(), userId, totalDistance);

      battleService.updateGpsData(
          request.getSessionId(),
          userId,
          request.getGps(),
          totalDistance
      );

      List<BattleRankingResDto> rankings = battleService.getRankings(request.getSessionId());

      // Redis Pub/Sub으로 순위 메시지 전송
      battleService.sendRankingMessage(request.getSessionId(), rankings);

      log.info("📡 순위 브로드캐스트: sessionId={}, 참가자={}명",
          request.getSessionId(), rankings.size());

    } catch (CustomException e) {
      // NotFoundException, ValidationException 등 모든 커스텀 Exception 처리
      log.error("❌ GPS 처리 실패 - {}: sessionId={}", 
          e.getErrorCode().getMessage(), request.getSessionId());
      battleService.sendErrorMessage(request.getSessionId(), e.getErrorCode());

    } catch (Exception e) {
      log.error("❌ GPS 처리 실패: sessionId={}", request.getSessionId(), e);
      battleService.sendErrorMessage(request.getSessionId(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}

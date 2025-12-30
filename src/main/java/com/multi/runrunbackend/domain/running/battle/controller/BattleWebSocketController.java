package com.multi.runrunbackend.domain.running.battle.controller;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.running.battle.dto.request.BattleGpsRequest;
import com.multi.runrunbackend.domain.running.battle.dto.request.BattleReadyRequest;
import com.multi.runrunbackend.domain.running.battle.dto.response.BattleRankingDto;
import com.multi.runrunbackend.domain.running.battle.dto.response.BattleUpdateResponse;
import com.multi.runrunbackend.domain.running.battle.service.BattleService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/battle/ready")
  public void handleReady(BattleReadyRequest request) {
    log.info("🎯 Ready 상태 변경 요청: sessionId={}, userId={}, isReady={}",
        request.getSessionId(), request.getUserId(), request.getIsReady());

    try {
      Long userId = request.getUserId();

      // toggleReady 반환값 사용 (allReady)
      boolean allReady = battleService.toggleReady(request.getSessionId(), userId,
          request.getIsReady());

      Map<String, Object> response = new HashMap<>();
      response.put("type", "BATTLE_READY");
      response.put("userId", userId);
      response.put("isReady", request.getIsReady());
      response.put("allReady", allReady);  // allReady 추가
      response.put("timestamp", LocalDateTime.now());

      messagingTemplate.convertAndSend(
          "/sub/battle/" + request.getSessionId() + "/ready",
          (Object) response
      );

      log.info("✅ Ready 상태 브로드캐스트: sessionId={}, userId={}, isReady={}, allReady={}",
          request.getSessionId(), userId, request.getIsReady(), allReady);

      // 모두 Ready면 자동 시작
      if (allReady) {
        log.info("🎉 모두 Ready! 자동 시작: sessionId={}", request.getSessionId());

        // 1초 대기 (UI 업데이트 시간)
        Thread.sleep(1000);

        // 배틀 시작
        battleService.startBattle(request.getSessionId());

        // 배틀 시작 알림
        Map<String, Object> startResponse = new HashMap<>();
        startResponse.put("type", "BATTLE_START");
        startResponse.put("sessionId", request.getSessionId());
        startResponse.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(
            "/sub/battle/" + request.getSessionId() + "/start",
            (Object) startResponse
        );

        log.info("🚩 배틀 시작 브로드캐스트: sessionId={}", request.getSessionId());

        // 초기 순위 전송 (0m로 초기화된 상태)
        List<BattleRankingDto> initialRankings = battleService.getRankings(request.getSessionId());
        BattleUpdateResponse initialUpdate = BattleUpdateResponse.builder()
            .type("BATTLE_UPDATE")
            .sessionId(request.getSessionId())
            .rankings(initialRankings)
            .timestamp(LocalDateTime.now())
            .build();

        messagingTemplate.convertAndSend(
            "/sub/battle/" + request.getSessionId() + "/ranking",
            (Object) initialUpdate
        );

        log.info("📊 초기 순위 전송: sessionId={}, 참가자={}명",
            request.getSessionId(), initialRankings.size());
      }

    } catch (InterruptedException e) {
      log.error("❌ Thread sleep 실패: sessionId={}", request.getSessionId(), e);
      Thread.currentThread().interrupt();

    } catch (Exception e) {
      log.error("❌ Ready 처리 실패: sessionId={}", request.getSessionId(), e);
      sendErrorMessage(request.getSessionId(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }


  @MessageMapping("/battle/gps")
  public void handleGps(BattleGpsRequest request) {
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

      List<BattleRankingDto> rankings = battleService.getRankings(request.getSessionId());

      BattleUpdateResponse response = BattleUpdateResponse.builder()
          .type("BATTLE_UPDATE")
          .sessionId(request.getSessionId())
          .rankings(rankings)
          .timestamp(LocalDateTime.now())
          .build();

      messagingTemplate.convertAndSend(
          "/sub/battle/" + request.getSessionId() + "/ranking",
          (Object) response
      );

      log.info("📡 순위 브로드캐스트: sessionId={}, 참가자={}명",
          request.getSessionId(), rankings.size());

    } catch (NotFoundException e) {
      log.error("❌ GPS 처리 실패 - 세션 없음: sessionId={}", request.getSessionId());
      sendErrorMessage(request.getSessionId(), e.getErrorCode());

    } catch (Exception e) {
      log.error("❌ GPS 처리 실패: sessionId={}", request.getSessionId(), e);
      sendErrorMessage(request.getSessionId(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private void sendErrorMessage(Long sessionId, ErrorCode errorCode) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("type", "ERROR");
    errorResponse.put("errorCode", errorCode.name());
    errorResponse.put("message", errorCode.getMessage());
    errorResponse.put("httpStatus", errorCode.getHttpStatus().value());
    errorResponse.put("timestamp", LocalDateTime.now());

    messagingTemplate.convertAndSend(
        "/sub/battle/" + sessionId + "/errors",
        (Object) errorResponse
    );

    log.info("📤 에러 메시지 전송: sessionId={}, errorCode={}", sessionId, errorCode.name());
  }
}

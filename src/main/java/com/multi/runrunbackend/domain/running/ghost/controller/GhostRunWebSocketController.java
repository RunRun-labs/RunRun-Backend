package com.multi.runrunbackend.domain.running.ghost.controller;

import com.multi.runrunbackend.domain.running.ghost.dto.req.GhostRunFinishReqDto;
import com.multi.runrunbackend.domain.running.ghost.service.GhostRunService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * @author : chang
 * @description : 고스트런 실시간 WebSocket 컨트롤러
 * @filename : GhostRunWebSocketController
 * @since : 2026-01-01
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class GhostRunWebSocketController {

  private final GhostRunService ghostRunService;

  /**
   * 실시간 GPS 데이터 수신 및 고스트 비교
   * 
   * 클라이언트 → /pub/ghost-run/{sessionId}/gps
   * 클라이언트 ← /sub/ghost-run/{sessionId}
   */
  @MessageMapping("/ghost-run/{sessionId}/gps")
  public void handleGpsUpdate(
      @DestinationVariable Long sessionId,
      Map<String, Object> gpsData
  ) {
    log.info("📍 GPS 수신: sessionId={}, distance={}km, time={}s",
        sessionId, gpsData.get("distance"), gpsData.get("elapsedTime"));

    // GPS 데이터 파싱
    Number distanceNum = (Number) gpsData.get("distance");
    Number elapsedTimeNum = (Number) gpsData.get("elapsedTime");

    double distance = distanceNum.doubleValue();  // km
    long elapsedTime = elapsedTimeNum.longValue();  // 초

    // Service에서 비교 계산 및 WebSocket 전송 처리
    ghostRunService.handleGpsUpdate(sessionId, distance, elapsedTime);
  }

  /**
   * 고스트런 종료
   * 
   * 클라이언트 → /pub/ghost-run/{sessionId}/finish
   * 클라이언트 ← /sub/ghost-run/{sessionId}/complete
   */
  @MessageMapping("/ghost-run/{sessionId}/finish")
  public void handleFinish(
      @DestinationVariable Long sessionId,
      @Payload GhostRunFinishReqDto request
  ) {
    Long userId = request.getUserId();
    
    log.info("🏁 완료 요청 수신: sessionId={}, userId={}", sessionId, userId);

    // Service에서 결과 저장 및 WebSocket 전송 처리
    ghostRunService.handleFinish(sessionId, userId, request);
  }
}

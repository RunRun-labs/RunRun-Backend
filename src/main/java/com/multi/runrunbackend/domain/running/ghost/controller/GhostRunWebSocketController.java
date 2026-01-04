package com.multi.runrunbackend.domain.running.ghost.controller;

import com.multi.runrunbackend.domain.running.ghost.dto.req.GhostRunFinishReqDto;
import com.multi.runrunbackend.domain.running.ghost.service.GhostRunService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 실시간 GPS 데이터 수신 및 고스트 비교
   * <p>
   * 클라이언트 → /pub/ghost-run/{sessionId}/gps 클라이언트 ← /sub/ghost-run/{sessionId}
   */
  @MessageMapping("/ghost-run/{sessionId}/gps")
  public void handleGpsUpdate(
      @DestinationVariable Long sessionId,
      Map<String, Object> gpsData
  ) {
    try {
      // GPS 데이터 파싱
      Number distanceNum = (Number) gpsData.get("distance");
      Number elapsedTimeNum = (Number) gpsData.get("elapsedTime");

      double distance = distanceNum.doubleValue();  // km
      long elapsedTime = elapsedTimeNum.longValue();  // 초

      // 고스트와 비교 계산
      Map<String, Object> comparison = ghostRunService.compareWithGhost(
          sessionId, distance, elapsedTime
      );

      // WebSocket으로 결과 전송
      messagingTemplate.convertAndSend(
          "/sub/ghost-run/" + sessionId,
          (Object) comparison
      );

    } catch (Exception e) {
      log.error("❌ GPS 처리 실패: sessionId={}", sessionId, e);

      messagingTemplate.convertAndSend(
          "/sub/ghost-run/" + sessionId + "/error",
          (Object) Map.of("error", e.getMessage())
      );
    }
  }

  /**
   * 고스트런 종료
   * <p>
   * 클라이언트 → /pub/ghost-run/{sessionId}/finish
   */
  @MessageMapping("/ghost-run/{sessionId}/finish")
  public void handleFinish(
      @DestinationVariable Long sessionId,
      @Payload GhostRunFinishReqDto request
  ) {
    try {
      Long userId = request.getUserId();

      if (userId == null) {
        log.error("❌ userId 없음: sessionId={}", sessionId);
        messagingTemplate.convertAndSend(
            "/sub/ghost-run/" + sessionId + "/error",
            (Object) Map.of("error", "userId가 필요합니다")
        );
        return;
      }

      log.info("🏁 고스트런 종료: sessionId={}, userId={}", sessionId, userId);

      // 러닝 결과 저장
      ghostRunService.finishGhostRun(sessionId, userId, request);

      // 성공 메시지 전송
      messagingTemplate.convertAndSend(
          "/sub/ghost-run/" + sessionId + "/complete",
          (Object) Map.of(
              "status", "COMPLETED",
              "message", "고스트런 완료!"
          )
      );

    } catch (Exception e) {
      log.error("❌ 종료 처리 실패: sessionId={}", sessionId, e);

      messagingTemplate.convertAndSend(
          "/sub/ghost-run/" + sessionId + "/error",
          (Object) Map.of("error", e.getMessage())
      );
    }
  }
}

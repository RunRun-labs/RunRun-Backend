package com.multi.runrunbackend.domain.running.controller;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.CustomException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.chat.service.RedisPublisher;
import com.multi.runrunbackend.domain.running.dto.GPSDataDTO;
import com.multi.runrunbackend.domain.running.dto.RunningStatsDTO;
import com.multi.runrunbackend.domain.running.service.RunningTrackingService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 런닝 WebSocket Controller - GPS 데이터 수신 및 브로드캐스트 (Redis Pub/Sub) - 예외 처리 및 에러 메시지 전송
 *
 * @author : chang
 * @since : 2024-12-25
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RunningWebSocketController {

  private final RunningTrackingService trackingService;
  private final RedisPublisher redisPublisher;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * GPS 데이터 수신 및 통계 브로드캐스트
   * <p>
   * 클라이언트 → /pub/running/gps (GPS 데이터) 서버 → Redis Channel "running:{sessionId}" Redis Subscriber →
   * /sub/running/{sessionId} (통계 브로드캐스트)
   *
   * @param gpsData GPS 데이터
   */
  @MessageMapping("/running/gps")
  public void handleGPSData(GPSDataDTO gpsData) {
    log.info("========================================");
    log.info("📡 GPS 데이터 수신됨!");
    log.info("sessionId: {}", gpsData.getSessionId());
    log.info("userId: {}", gpsData.getUserId());
    log.info("distance: {}km", gpsData.getTotalDistance());
    log.info("========================================");

    try {

      // 1. GPS 데이터 처리 및 통계 계산
      RunningStatsDTO stats = trackingService.processGPSData(gpsData);

      // 2. Redis Pub/Sub으로 모든 서버에 브로드캐스트
      String channel = "running:" + gpsData.getSessionId();
      redisPublisher.publishObject(channel, stats);

      log.info("📊 통계 브로드캐스트: sessionId={}, avgPace={}, distance={}km, remaining={}km",
          stats.getSessionId(),
          stats.getTeamAveragePace(),
          stats.getTotalDistance(),
          stats.getRemainingDistance());

    } catch (NotFoundException e) {
      log.error("❌ GPS 처리 실패 - 리소스 없음: sessionId={}, userId={}, error={}",
          gpsData.getSessionId(), gpsData.getUserId(), e.getMessage());
      sendErrorMessage(gpsData.getSessionId(), e.getErrorCode());

    } catch (BadRequestException e) {
      log.error("❌ GPS 처리 실패 - 잘못된 요청: sessionId={}, userId={}, error={}",
          gpsData.getSessionId(), gpsData.getUserId(), e.getMessage());
      sendErrorMessage(gpsData.getSessionId(), e.getErrorCode());

    } catch (BusinessException e) {
      log.error("❌ GPS 처리 실패 - 비즈니스 로직 오류: sessionId={}, userId={}, error={}",
          gpsData.getSessionId(), gpsData.getUserId(), e.getMessage());
      sendErrorMessage(gpsData.getSessionId(), e.getErrorCode());

    } catch (CustomException e) {
      log.error("❌ GPS 처리 실패 - 커스텀 예외: sessionId={}, userId={}, error={}",
          gpsData.getSessionId(), gpsData.getUserId(), e.getMessage());
      sendErrorMessage(gpsData.getSessionId(), e.getErrorCode());

    } catch (Exception e) {
      log.error("❌ GPS 처리 실패 - 예상치 못한 오류: sessionId={}, userId={}",
          gpsData.getSessionId(), gpsData.getUserId(), e);
      sendErrorMessage(gpsData.getSessionId(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 세션 참여자들에게 에러 메시지 전송
   *
   * @param sessionId 세션 ID
   * @param errorCode 에러 코드
   */
  private void sendErrorMessage(Long sessionId, ErrorCode errorCode) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("type", "ERROR");
    errorResponse.put("errorCode", errorCode.name());
    errorResponse.put("message", errorCode.getMessage());
    errorResponse.put("httpStatus", errorCode.getHttpStatus().value());
    errorResponse.put("timestamp", System.currentTimeMillis());

    // 세션의 모든 참여자에게 에러 메시지 브로드캐스트
    messagingTemplate.convertAndSend(
        "/sub/running/" + sessionId + "/errors",
        (Object) errorResponse
    );

    log.info("📤 에러 메시지 전송: sessionId={}, errorCode={}, message={}",
        sessionId, errorCode.name(), errorCode.getMessage());
  }
}

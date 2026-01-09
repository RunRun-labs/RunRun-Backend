package com.multi.runrunbackend.domain.running.ghost.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.service.RunningResultService;
import com.multi.runrunbackend.domain.running.ghost.dto.req.GhostRunFinishReqDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : chang
 * @description : 고스트런 비즈니스 로직 서비스 (기존 MatchSession 연동)
 * @filename : GhostRunService
 * @since : 2026-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GhostRunService {

  private final RunningResultRepository runningResultRepository;
  private final RunningResultService runningResultService;
  private final MatchSessionRepository matchSessionRepository;
  private final UserRepository userRepository;
  private final GhostCompareService ghostCompareService;
  private final SimpMessagingTemplate messagingTemplate;

  // 세션별 고스트 데이터 (메모리 캐싱)
  private final Map<Long, GhostSessionData> ghostSessions = new ConcurrentHashMap<>();

  /**
   * 고스트 세션 데이터 (메모리 저장용)
   */
  private static class GhostSessionData {

    List<Map<String, Object>> splitPace;
    double totalDistance;
    int totalTime;

    GhostSessionData(List<Map<String, Object>> splitPace, double totalDistance, int totalTime) {
      this.splitPace = splitPace;
      this.totalDistance = totalDistance;
      this.totalTime = totalTime;
    }
  }

  /**
   * 기존 MatchSession을 사용한 고스트 세션 초기화
   *
   * @param sessionId 기존에 생성된 MatchSession ID
   */
  @Transactional(readOnly = true)
  public void initializeGhostSession(Long sessionId) {
    // 세션 조회 (RunningResult JOIN FETCH)
    MatchSession session = matchSessionRepository.findByIdWithRunningResult(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 고스트 기록 조회
    RunningResult ghostRecord = session.getRunningResult();
    if (ghostRecord == null) {
      throw new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND);
    }

    // splitPace 데이터 가져오기
    List<Map<String, Object>> ghostSplits = ghostRecord.getSplitPace();

    // 메모리에 저장
    GhostSessionData sessionData = new GhostSessionData(
        ghostSplits,
        ghostRecord.getTotalDistance().doubleValue(),
        ghostRecord.getTotalTime()
    );
    ghostSessions.put(sessionId, sessionData);

    log.info("🏃 고스트 세션 초기화: sessionId={}", sessionId);
  }

  /**
   * WebSocket GPS 업데이트 처리 (Controller에서 호출)
   *
   * @param sessionId     MatchSession ID
   * @param myDistance    내가 뛴 거리 (km)
   * @param myElapsedTime 내가 달린 시간 (초)
   */
  //실시간 GPS 데이터를 받아서 고스트와 비교하고, 결과를 WebSocket으로 클라이언트에게 전송하는 메서드
  public void handleGpsUpdate(Long sessionId, double myDistance, long myElapsedTime) {
    try {
      // 고스트와 비교 계산
      Map<String, Object> comparison = compareWithGhost(sessionId, myDistance, myElapsedTime);

      // WebSocket으로 결과 전송
      sendComparisonMessage(sessionId, comparison);

      log.info("📊 고스트 비교 완료: sessionId={}, status={}, diff={}m",
          sessionId, comparison.get("status"), comparison.get("distanceDiffMeters"));

    } catch (Exception e) {
      log.error("❌ GPS 처리 실패: sessionId={}", sessionId, e);
      sendErrorMessage(sessionId, e.getMessage());
    }
  }

  /**
   * 실시간 GPS로 고스트 비교 (내부 로직)
   *
   * @param sessionId     MatchSession ID
   * @param myDistance    내가 뛴 거리 (km)
   * @param myElapsedTime 내가 달린 시간 (초)
   * @return 비교 결과
   */
  //메모리에서 고스트 데이터를 가져와서 GhostCompareService에게 비교 계산을 시키는 메서드
  private Map<String, Object> compareWithGhost(
      Long sessionId,
      double myDistance,
      long myElapsedTime
  ) {
    // 메모리에서 고스트 데이터 가져오기
    GhostSessionData sessionData = ghostSessions.get(sessionId);

    // 없으면 DB에서 로드 (자동 초기화)
    if (sessionData == null) {
      log.warn("⚠️ 메모리에 없음, DB에서 로드: sessionId={}", sessionId);
      initializeGhostSession(sessionId);
      sessionData = ghostSessions.get(sessionId);
    }

    // 비교 계산
    return ghostCompareService.compare(
        myDistance,
        myElapsedTime,
        sessionData.splitPace,
        sessionData.totalDistance,
        sessionData.totalTime
    );
  }

  /**
   * 고스트 세션 종료
   *
   * @param sessionId 세션 ID
   */
  public void endGhostSession(Long sessionId) {
    ghostSessions.remove(sessionId);
    log.info("🏁 고스트 세션 종료: sessionId={}", sessionId);
  }

  /**
   * WebSocket 완료 처리 (Controller에서 호출)
   *
   * @param sessionId 세션 ID
   * @param userId    사용자 ID
   * @param request   완료 데이터
   */
  //고스트런 완료 시  메모리를 정리하고, 완료 메시지를 전송
  @Transactional
  public void handleFinish(Long sessionId, Long userId, GhostRunFinishReqDto request) {
    try {
      // userId 검증
      if (userId == null) {
        log.error("❌ userId 없음: sessionId={}", sessionId);
        sendErrorMessage(sessionId, "userId가 필요합니다");
        return;
      }

      log.info("🏁 고스트런 종료: sessionId={}, userId={}", sessionId, userId);

      // 러닝 결과 저장
      RunningResult result = finishGhostRun(sessionId, userId, request);

      // 성공 메시지 전송
      sendCompleteMessage(sessionId);

      log.info("✅ 고스트런 완료: sessionId={}, resultId={}", sessionId, result.getId());

    } catch (Exception e) {
      log.error("❌ 종료 처리 실패: sessionId={}", sessionId, e);
      sendErrorMessage(sessionId, e.getMessage());
    }
  }

  /**
   * 고스트런 완료 및 결과 저장 (내부 로직)
   *
   * @param sessionId 세션 ID
   * @param userId    사용자 ID
   * @param request   완료 데이터
   * @return 저장된 RunningResult
   */
  //런닝결과 저장
  private RunningResult finishGhostRun(
      Long sessionId,
      Long userId,
      GhostRunFinishReqDto request
  ) {
    log.info("🏁 고스트런 완료: sessionId={}, userId={}", sessionId, userId);

    // 세션 조회
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    // RunningResult 생성
    RunningResult result = RunningResult.builder()
        .user(user)
        .totalDistance(BigDecimal.valueOf(request.getTotalDistance()))
        .totalTime(request.getTotalTime())
        .avgPace(BigDecimal.valueOf(request.getAvgPace()))
        .startedAt(LocalDateTime.now())
        .splitPace(new ArrayList<>())
        .runningType(RunningType.GHOST)
        .runStatus(RunStatus.COMPLETED)
        .build();

    RunningResult savedResult = runningResultService.saveAndUpdateAverage(result);

    // 메모리 정리
    endGhostSession(sessionId);

    return savedResult;
  }

  /**
   * WebSocket 비교 결과 메시지 전송
   */
  private void sendComparisonMessage(Long sessionId, Map<String, Object> comparison) {
    messagingTemplate.convertAndSend(
        "/sub/ghost-run/" + sessionId,
        (Object) comparison
    );
  }

  /**
   * WebSocket 완료 메시지 전송
   */
  private void sendCompleteMessage(Long sessionId) {
    messagingTemplate.convertAndSend(
        "/sub/ghost-run/" + sessionId + "/complete",
        (Object) Map.of(
            "status", "COMPLETED",
            "message", "고스트런 완료!"
        )
    );
  }

  /**
   * WebSocket 에러 메시지 전송
   */
  private void sendErrorMessage(Long sessionId, String errorMessage) {
    messagingTemplate.convertAndSend(
        "/sub/ghost-run/" + sessionId + "/error",
        (Object) Map.of("error", errorMessage)
    );
  }
}

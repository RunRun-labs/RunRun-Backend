package com.multi.runrunbackend.domain.running.ghost.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
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
  private final MatchSessionRepository matchSessionRepository;
  private final UserRepository userRepository;
  private final GhostCompareService ghostCompareService;

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
   * 실시간 GPS로 고스트 비교
   *
   * @param sessionId     MatchSession ID
   * @param myDistance    내가 뛴 거리 (km)
   * @param myElapsedTime 내가 달린 시간 (초)
   * @return 비교 결과
   */
  public Map<String, Object> compareWithGhost(
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
   * 고스트런 완료 및 결과 저장
   *
   * @param sessionId 세션 ID
   * @param userId    사용자 ID
   * @param request   완료 데이터
   * @return 저장된 RunningResult
   */
  @Transactional
  public RunningResult finishGhostRun(
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

    RunningResult savedResult = runningResultRepository.save(result);

    // 메모리 정리
    endGhostSession(sessionId);

    return savedResult;
  }
}

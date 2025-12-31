package com.multi.runrunbackend.domain.running.battle.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.BattleResultRepository;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.running.battle.dto.request.BattleGpsRequest.GpsData;
import com.multi.runrunbackend.domain.running.battle.dto.response.BattleRankingDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : chang
 * @description : 온라인 배틀 비즈니스 로직 서비스
 * @filename : BattleService
 * @since : 2025-12-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleService {

  private final BattleRedisService battleRedisService;
  private final MatchSessionRepository matchSessionRepository;
  private final SessionUserRepository sessionUserRepository;
  private final UserRepository userRepository;
  private final RunningResultRepository runningResultRepository;
  private final BattleResultRepository battleResultRepository;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Ready 상태 토글
   */
  @Transactional
  public boolean toggleReady(Long sessionId, Long userId, Boolean isReady) {
    SessionUser sessionUser = sessionUserRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    sessionUser.updateReady(isReady);
    sessionUserRepository.save(sessionUser);

    log.info("✅ Ready 상태 변경: sessionId={}, userId={}, isReady={}",
        sessionId, userId, isReady);

    return checkAllReady(sessionId);
  }

  /**
   * 모든 참가자 Ready 확인
   */
  public boolean checkAllReady(Long sessionId) {
    List<SessionUser> participants = sessionUserRepository.findActiveUsersBySessionId(sessionId);

    if (participants.isEmpty()) {
      return false;
    }

    boolean allReady = participants.stream().allMatch(SessionUser::isReady);

    if (allReady) {
      log.info("✅ 모든 참가자 Ready 완료: sessionId={}, 참가자={}명",
          sessionId, participants.size());
    }

    return allReady;
  }

  /**
   * 배틀 시작
   */
  @Transactional
  public void startBattle(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    List<SessionUser> participants = sessionUserRepository.findActiveUsersBySessionId(sessionId);
    boolean allReady = participants.stream().allMatch(SessionUser::isReady);

    if (!allReady) {
      throw new IllegalStateException("모든 참가자가 Ready 상태가 아닙니다.");
    }

    session.updateStatus(SessionStatus.IN_PROGRESS);
    matchSessionRepository.save(session);

    for (SessionUser participant : participants) {
      User user = participant.getUser();
      battleRedisService.initializeBattleUser(sessionId, user.getId(), user.getName());
      log.info("✅ 배틀 참가자 초기화: sessionId={}, userId={}, username={}",
          sessionId, user.getId(), user.getName());
    }

    log.info("🏁 배틀 시작: sessionId={}, 참가자={}명", sessionId, participants.size());
  }

  /**
   * 타임아웃 처리
   */
  @Transactional
  public boolean handleTimeout(Long sessionId) {
    log.info("⏰ 타임아웃 처리 시작: sessionId={}", sessionId);

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 이미 시작된 경우 - 자동 시작으로 처리됨
    if (session.getStatus() == SessionStatus.IN_PROGRESS) {
      log.info("✅ 이미 배틀 시작됨 (자동 시작): sessionId={}", sessionId);
      return true;  // 시작됨으로 반환
    }

    // 이미 종료되거나 취소된 경우
    if (session.getStatus() != SessionStatus.STANDBY) {
      log.warn("⚠️ 세션 상태가 STANDBY가 아님: sessionId={}, status={}",
          sessionId, session.getStatus());
      return false;
    }

    // STANDBY 상태 - 타임아웃 처리 필요
    int kickedCount = kickNotReadyUsers(sessionId);

    List<SessionUser> remainingUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);
    int remainingCount = remainingUsers.size();

    log.info("📊 타임아웃 결과: 강퇴={}명, 남은 인원={}명", kickedCount, remainingCount);

    if (remainingCount >= 2) {
      log.info("🏁 남은 참가자끼리 배틀 시작: sessionId={}", sessionId);
      startBattle(sessionId);
      return true;
    }

    log.info("❌ 참가자 부족으로 매치 취소: sessionId={}", sessionId);
    cancelMatch(sessionId);
    return false;
  }

  /**
   * Ready 안 한 사람 강퇴
   */
  @Transactional
  public int kickNotReadyUsers(Long sessionId) {
    List<SessionUser> allUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

    List<SessionUser> notReadyUsers = allUsers.stream()
        .filter(su -> !su.isReady())
        .toList();

    if (notReadyUsers.isEmpty()) {
      log.info("✅ 모두 Ready 상태임, 강퇴 대상 없음");
      return 0;
    }

    for (SessionUser user : notReadyUsers) {
      user.delete();
      sessionUserRepository.save(user);
      log.info("🚪 참가자 강퇴: sessionId={}, userId={}, name={}",
          sessionId, user.getUser().getId(), user.getUser().getName());
    }

    return notReadyUsers.size();
  }

  /**
   * 매치 취소
   */
  @Transactional
  public void cancelMatch(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // CANCELLED 대신 삭제 플래그 사용
    // session.updateStatus(SessionStatus.CANCELLED);
    // 또는 COMPLETED로 변경
    session.updateStatus(SessionStatus.COMPLETED);
    matchSessionRepository.save(session);

    log.info("❌ 매치 취소: sessionId={}", sessionId);
  }

  /**
   * GPS 데이터 업데이트
   */
  public void updateGpsData(Long sessionId, Long userId, GpsData gps, Double totalDistance) {
    battleRedisService.updateGpsData(sessionId, userId, gps, totalDistance);

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // targetDistance는 km 단위이므로 미터로 변환
    Double targetDistanceInMeters = session.getTargetDistance() * 1000;

    if (totalDistance >= targetDistanceInMeters) {
      battleRedisService.finishUser(sessionId, userId);
      log.info("🏆 참가자 완주: sessionId={}, userId={}, distance={}m",
          sessionId, userId, totalDistance);
    }
  }

  /**
   * 전체 순위 조회
   */
  public List<BattleRankingDto> getRankings(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // targetDistance는 km 단위이므로 미터로 변환하여 전달
    Double targetDistanceInMeters = session.getTargetDistance() * 1000;

    return battleRedisService.getRankings(sessionId, targetDistanceInMeters);
  }

  /**
   * 배틀 종료
   */
  @Transactional
  public void finishBattle(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    session.updateStatus(SessionStatus.COMPLETED);
    matchSessionRepository.save(session);

    // ✅ 배틀 결과 DB 저장
    saveBattleResults(sessionId, session.getCreatedAt());

    // ✅ 모든 참가자 완주 알림 (WebSocket)
    sendBattleCompleteMessage(sessionId);

    log.info("🏁 배틀 종료 및 결과 저장: sessionId={}", sessionId);
  }

  /**
   * 배틀 결과 DB 저장
   */
  private void saveBattleResults(Long sessionId, LocalDateTime startedAt) {
    // 1. Redis에서 순위 조회
    List<BattleRankingDto> rankings = getRankings(sessionId);

    if (rankings.isEmpty()) {
      log.warn("⚠️ 순위 데이터 없음: sessionId={}", sessionId);
      return;
    }

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 2. 각 참가자의 결과 저장
    for (BattleRankingDto ranking : rankings) {
      User user = userRepository.findById(ranking.getUserId())
          .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

      // RunningResult 생성
      RunningResult runningResult = RunningResult.builder()
          .user(user)
          .course(null)  // 배틀은 코스 없음
          .totalDistance(BigDecimal.valueOf(ranking.getTotalDistance() / 1000.0))  // km
          .totalTime(calculateTotalTime(ranking))
          .avgPace(parsePaceToBigDecimal(ranking.getCurrentPace()))
          .startedAt(startedAt)
          .runStatus(ranking.getIsFinished() ? RunStatus.COMPLETED : RunStatus.GIVE_UP)
          .splitPace(new ArrayList<>())  // 구간 페이스 비어있음
          .runningType(RunningType.ONLINEBATTLE)
          .build();

      runningResultRepository.save(runningResult);

      // BattleResult 생성
      BattleResult battleResult = BattleResult.builder()
          .session(session)
          .user(user)
          .ranking(ranking.getRank())
          .distanceType(determineDistanceType(session.getTargetDistance()))
          .previousRating(1500)  // TODO: User에 rating 필드 추가 후 user.getRating()으로 변경
          .currentRating(1500)   // TODO: 레이팅 계산 로직 추가
          .runningResult(runningResult)
          .build();

      battleResultRepository.save(battleResult);

      log.info("✅ 배틀 결과 저장: sessionId={}, userId={}, rank={}, distance={}km",
          sessionId, user.getId(), ranking.getRank(), runningResult.getTotalDistance());
    }
  }

  /**
   * 총 시간 계산 (초)
   */
  private Integer calculateTotalTime(BattleRankingDto ranking) {
    double distanceKm = ranking.getTotalDistance() / 1000.0;
    String[] paceParts = ranking.getCurrentPace().split(":");
    int paceMinutes = Integer.parseInt(paceParts[0]);
    int paceSeconds = Integer.parseInt(paceParts[1]);
    int paceInSeconds = paceMinutes * 60 + paceSeconds;
    return (int) (distanceKm * paceInSeconds);
  }

  /**
   * 페이스 문자열 -> BigDecimal (분/km)
   */
  private BigDecimal parsePaceToBigDecimal(String pace) {
    String[] parts = pace.split(":");
    int minutes = Integer.parseInt(parts[0]);
    int seconds = Integer.parseInt(parts[1]);
    double paceDecimal = minutes + (seconds / 60.0);
    return BigDecimal.valueOf(paceDecimal);
  }

  /**
   * 거리 타입 결정
   */
  private DistanceType determineDistanceType(Double targetDistance) {
    // targetDistance는 이미 km 단위
    int km = targetDistance.intValue();
    return switch (km) {
      case 3 -> DistanceType.KM_3;
      case 5 -> DistanceType.KM_5;
      case 10 -> DistanceType.KM_10;
      default -> DistanceType.KM_5;
    };
  }

  /**
   * 배틀 종료 메시지 전송
   */
  private void sendBattleCompleteMessage(Long sessionId) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BATTLE_COMPLETE");
    message.put("sessionId", sessionId);
    message.put("timestamp", LocalDateTime.now());

    messagingTemplate.convertAndSend(
        "/sub/battle/" + sessionId + "/complete",
        (Object) message  // ✅ 명시적 캐스팅
    );

    log.info("🏁 배틀 종료 메시지 전송: sessionId={}", sessionId);
  }


  /**
   * 배틀 결과 조회
   */
  public Map<String, Object> getBattleResult(Long sessionId, Long userId) {
    // 세션 조회
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 순위 조회
    List<BattleRankingDto> rankings = getRankings(sessionId);

    // 내 데이터 찾기
    BattleRankingDto myData = rankings.stream()
        .filter(r -> r.getUserId().equals(userId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 결과 데이터 구성
    Map<String, Object> result = new HashMap<>();
    result.put("sessionId", sessionId);
    result.put("targetDistance", session.getTargetDistance());
    result.put("myRank", myData.getRank());
    result.put("totalDistance", myData.getTotalDistance());
    result.put("finishTime", calculateFinishTime(myData));
    result.put("avgPace", myData.getCurrentPace());
    result.put("rankings", rankings);

    log.info("📊 배틀 결과: sessionId={}, userId={}, rank={}",
        sessionId, userId, myData.getRank());

    return result;
  }

  /**
   * 완주 시간 계산
   */
  private Long calculateFinishTime(BattleRankingDto ranking) {
    double distanceKm = ranking.getTotalDistance() / 1000.0;
    String[] paceParts = ranking.getCurrentPace().split(":");
    int paceMinutes = Integer.parseInt(paceParts[0]);
    int paceSeconds = Integer.parseInt(paceParts[1]);
    int paceInSeconds = paceMinutes * 60 + paceSeconds;

    long totalSeconds = (long) (distanceKm * paceInSeconds);
    return totalSeconds * 1000;
  }

  /**
   * 참가자 완주 처리 및 전체 완료 체크
   */
  @Transactional
  public void finishUserAndCheckComplete(Long sessionId, Long userId) {
    battleRedisService.finishUser(sessionId, userId);
    log.info("🏆 참가자 완주 처리: sessionId={}, userId={}", sessionId, userId);

    List<BattleRankingDto> rankings = getRankings(sessionId);
    boolean allFinished = rankings.stream().allMatch(BattleRankingDto::getIsFinished);

    if (allFinished) {
      log.info("✅ 모든 참가자 완주 - 배틀 종료: sessionId={}", sessionId);
      finishBattle(sessionId);
    }
  }

  /**
   * 참가자 포기 처리
   */
  @Transactional
  public void quitBattle(Long sessionId, Long userId) {
    SessionUser sessionUser = sessionUserRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    sessionUser.delete();
    sessionUserRepository.save(sessionUser);
    log.info("🚪 참가자 포기: sessionId={}, userId={}", sessionId, userId);

    List<SessionUser> remainingUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);
    if (remainingUsers.size() < 2) {
      log.info("⚠️ 참가자 부족으로 배틀 종료: sessionId={}", sessionId);
      finishBattle(sessionId);
    }
  }
}

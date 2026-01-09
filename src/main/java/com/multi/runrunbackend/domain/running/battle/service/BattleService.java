package com.multi.runrunbackend.domain.running.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.ValidationException;
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
import com.multi.runrunbackend.domain.match.service.RunningResultService;
import com.multi.runrunbackend.domain.rating.entity.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.rating.service.DistanceRatingService;
import com.multi.runrunbackend.domain.running.battle.dto.TimeoutDto;
import com.multi.runrunbackend.domain.running.battle.dto.req.BattleGpsReqDto.GpsData;
import com.multi.runrunbackend.domain.running.battle.dto.res.BattleRankingResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
  private final RunningResultService runningResultService;
  private final BattleResultRepository battleResultRepository;
  private final DistanceRatingRepository distanceRatingRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, Object> redisPubSubTemplate;
  private final ObjectMapper objectMapper;
  private final DistanceRatingService distanceRatingService;

  // ✅ 타임아웃 스케줄러
  private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(10);

  /**
   * Ready 상태 토글
   */
  @Transactional
  public boolean toggleReady(Long sessionId, Long userId, Boolean isReady) {
    // null 체크 - 커스텀 Exception 사용
    if (isReady == null) {
      throw new ValidationException(ErrorCode.INVALID_READY_STATUS);
    }

    SessionUser sessionUser = sessionUserRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    sessionUser.updateReady(isReady);  // primitive boolean으로 자동 언박싱
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

    log.info("🔥🔥🔥 startBattle 호출: sessionId={}, 참가자={}명", sessionId, participants.size());
    for (SessionUser p : participants) {
      log.info("  - userId={}, username={}, ready={}",
          p.getUser().getId(), p.getUser().getName(), p.isReady());
    }

    boolean allReady = participants.stream().allMatch(SessionUser::isReady);

    if (!allReady) {
      throw new ValidationException(ErrorCode.ALL_USERS_NOT_READY);
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

    // ✅ 배틀 시작 메시지 전송
    sendBattleStartMessage(sessionId);
  }

  /**
   * 타임아웃 처리
   *
   * @return Map<String, Object> - sessionId, started, alreadyStarted 포함
   */
  @Transactional
  public Map<String, Object> handleTimeout(Long sessionId) {
    log.info("⏰ 타임아웃 처리 시작: sessionId={}", sessionId);

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 이미 시작된 경우 - 자동 시작으로 처리됨
    if (session.getStatus() == SessionStatus.IN_PROGRESS) {
      log.info("✅ 이미 배틀 시작됨 (자동 시작): sessionId={}", sessionId);
      Map<String, Object> result = new HashMap<>();
      result.put("sessionId", sessionId);
      result.put("started", true);
      result.put("alreadyStarted", true);
      return result;
    }

    // 이미 종료되거나 취소된 경우
    if (session.getStatus() != SessionStatus.STANDBY) {
      log.warn("⚠️ 세션 상태가 STANDBY가 아님: sessionId={}, status={}",
          sessionId, session.getStatus());
      Map<String, Object> result = new HashMap<>();
      result.put("sessionId", sessionId);
      result.put("started", false);
      result.put("alreadyStarted", false);
      return result;
    }

    // STANDBY 상태 - 타임아웃 처리 필요
    int kickedCount = kickNotReadyUsers(sessionId);

    List<SessionUser> remainingUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);
    int remainingCount = remainingUsers.size();

    log.info("📊 타임아웃 결과: 강퇴={}명, 남은 인원={}명", kickedCount, remainingCount);

    Map<String, Object> result = new HashMap<>();
    result.put("sessionId", sessionId);
    result.put("alreadyStarted", false);

    if (remainingCount >= 2) {
      log.info("🏁 남은 참가자끼리 배틀 시작: sessionId={}", sessionId);
      startBattle(sessionId);
      result.put("started", true);

      // WebSocket 메시지 전송
      sendTimeoutStartMessage(sessionId);
      sendBattleStartMessage(sessionId);

    } else {
      log.info("❌ 참가자 부족으로 매치 취소: sessionId={}", sessionId);
      cancelMatch(sessionId);
      result.put("started", false);

      // WebSocket 메시지 전송
      sendTimeoutCancelMessage(sessionId);
    }

    return result;
  }

  /**
   * Ready 상태 메시지 전송 (Redis Pub/Sub)
   */
  public void sendReadyMessage(Long sessionId, Long userId, Boolean isReady, boolean allReady) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BATTLE_READY");
    message.put("userId", userId);
    message.put("isReady", isReady);
    message.put("allReady", allReady);
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/ready", message);

    log.info("✅ Ready 메시지 발행: sessionId={}, userId={}, isReady={}, allReady={}",
        sessionId, userId, isReady, allReady);
  }

  /**
   * 순위 업데이트 메시지 전송 (Redis Pub/Sub)
   */
  public void sendRankingMessage(Long sessionId, List<BattleRankingResDto> rankings) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BATTLE_UPDATE");
    message.put("sessionId", sessionId);
    message.put("rankings", rankings);
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/ranking", message);

    log.info("📊 순위 메시지 발행: sessionId={}, 참가자={}명",
        sessionId, rankings.size());
  }

  /**
   * 에러 메시지 전송 (Redis Pub/Sub)
   */
  public void sendErrorMessage(Long sessionId, ErrorCode errorCode) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "ERROR");
    message.put("errorCode", errorCode.name());
    message.put("message", errorCode.getMessage());
    message.put("httpStatus", errorCode.getHttpStatus().value());
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/errors", message);

    log.info("📤 에러 메시지 발행: sessionId={}, errorCode={}",
        sessionId, errorCode.name());
  }

  /**
   * 타임아웃 시작 메시지 전송
   */
  private void sendTimeoutStartMessage(Long sessionId) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "TIMEOUT_START");
    message.put("message", "일부 참가자가 강퇴되었습니다. 배틀을 시작합니다.");
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/timeout", message);
  }

  /**
   * 배틀 시작 메시지 전송
   */
  private void sendBattleStartMessage(Long sessionId) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BATTLE_START");
    message.put("sessionId", sessionId);
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/start", message);
  }

  /**
   * 타임아웃 취소 메시지 전송
   */
  private void sendTimeoutCancelMessage(Long sessionId) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "TIMEOUT_CANCEL");
    message.put("message", "참가자가 부족하여 매치가 취소되었습니다.");
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/timeout", message);
  }

  /**
   * Redis Pub/Sub을 통한 메시지 발행 (다중 서버 환경 지원)
   */
  private void publishToRedis(String destination, Object message) {
    try {
      Map<String, Object> redisMessage = new HashMap<>();
      redisMessage.put("destination", destination);
      redisMessage.put("message", message);

      String channel = "battle:" + destination.hashCode();
      String payload = objectMapper.writeValueAsString(redisMessage);

      redisPubSubTemplate.convertAndSend(channel, payload);

      log.info("📤 [Redis Pub] 메시지 발행 - destination: {}, channel: {}", destination, channel);

    } catch (Exception e) {
      log.error("❌ Redis Pub 실패: destination={}", destination, e);
    }
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

      // ✅ 모든 참가자 완주 확인
      checkAndFinishBattle(sessionId);
    }
  }

  /**
   * 모든 참가자 완주 확인 및 배틀 종료
   */
  private void checkAndFinishBattle(Long sessionId) {
    List<BattleRankingResDto> rankings = getRankings(sessionId);

    // ✅ 완주한 참가자 수 확인
    long finishedCount = rankings.stream()
        .filter(BattleRankingResDto::getIsFinished)
        .count();

    boolean allFinished = rankings.stream().allMatch(BattleRankingResDto::getIsFinished);

    log.info("📊 완주 상태 확인: sessionId={}, 완주={}/{}명, allFinished={}",
        sessionId, finishedCount, rankings.size(), allFinished);

    // ✅ 첫 번째 완주자 발생 시 타임아웃 시작
    if (finishedCount == 1) {
      TimeoutDto existingTimeout = battleRedisService.getTimeoutData(sessionId);
      if (existingTimeout == null) {
        // 거리에 따른 타임아웃 설정 (고정 30초)
        MatchSession session = matchSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        int timeoutSeconds = 30;  // ✅ 고정 30초
        battleRedisService.setFirstFinishTime(sessionId, timeoutSeconds);
        sendTimeoutStartMessage(sessionId, timeoutSeconds);

        log.info("🏆 첫 완주자 등장 - 타임아웃 시작: sessionId={}, timeout={}초",
            sessionId, timeoutSeconds);

        // ✅ 30초 후 자동 종료 예약
        timeoutScheduler.schedule(() -> {
          try {
            log.info("⏰ 타임아웃 만료! 배틀 자동 종료: sessionId={}", sessionId);
            executeTimeoutFinish(sessionId);
          } catch (Exception e) {
            log.error("❌ 타임아웃 종료 실패: sessionId={}", sessionId, e);
          }
        }, timeoutSeconds, TimeUnit.SECONDS);

        log.info("✅ 타임아웃 스케줄러 등록: {}초 후 자동 종료", timeoutSeconds);
      }
    }

    // ✅ 모든 참가자 완주 확인
    if (allFinished) {
      MatchSession session = matchSessionRepository.findById(sessionId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

      if (session.getStatus() == SessionStatus.IN_PROGRESS) {
        log.info("✅ 모든 참가자 완주 감지 - 배틀 종료: sessionId={}", sessionId);
        finishBattle(sessionId);
      } else {
        log.info("ℹ️ 이미 종료된 배틀: sessionId={}, status={}", sessionId, session.getStatus());
      }
      return;
    }

    log.info("ℹ️ 아직 완주 안 한 참가자 있음: sessionId={}", sessionId);
    for (BattleRankingResDto ranking : rankings) {
      log.info("  - userId={}, username={}, finished={}, distance={}m",
          ranking.getUserId(), ranking.getUsername(), ranking.getIsFinished(),
          ranking.getTotalDistance());
    }
  }

  /**
   * 전체 순위 조회
   */
  public List<BattleRankingResDto> getRankings(Long sessionId) {
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
    finishBattle(sessionId, false);
  }

  /**
   * 배틀 종료 (타임아웃 여부 구분)
   */
  @Transactional
  public void finishBattle(Long sessionId, boolean isTimeout) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // ✅ 이미 종료된 경우 중복 처리 방지
    if (session.getStatus() == SessionStatus.COMPLETED) {
      log.info("ℹ️ 이미 종료된 배틀 - 스킵: sessionId={}", sessionId);
      return;
    }

    session.updateStatus(SessionStatus.COMPLETED);
    matchSessionRepository.save(session);

    // ✅ 배틀 결과 DB 저장 (타임아웃 여부 전달)
    saveBattleResults(sessionId, session.getCreatedAt(), isTimeout);

    // ✅ 모든 참가자 완주 알림 (WebSocket)
    sendBattleCompleteMessage(sessionId);

    log.info("🏁 배틀 종료 및 결과 저장: sessionId={}, isTimeout={}", sessionId, isTimeout);
  }

  /**
   * 배틀 결과 DB 저장
   */
  private void saveBattleResults(Long sessionId, LocalDateTime startedAt, boolean isTimeout) {
    log.info("🔥🔥🔥 saveBattleResults 호출: sessionId={}, isTimeout={}", sessionId, isTimeout);

    List<BattleRankingResDto> rankings = getRankings(sessionId);

    log.info("🔥🔥🔥 getRankings 반환 결과: {}명", rankings.size());

    if (rankings.isEmpty()) {
      log.warn(" Redis 데이터 없음 - DB 복구 시도: sessionId={}", sessionId);
      rankings = recoverRankingsFromDB(sessionId);
    }

    if (rankings.isEmpty()) {
      log.error(" DB 복구도 실패 - 최소한 데이터 저장: sessionId={}", sessionId);
      saveMinimalBattleResults(sessionId, startedAt);
      return;
    }

    // ✅ 전체 순위 로깅
    log.info("📊 전체 순위 ({}명):", rankings.size());
    for (BattleRankingResDto r : rankings) {
      log.info("  - rank={}, userId={}, username={}, status={}, finished={}, distance={}m",
          r.getRank(), r.getUserId(), r.getUsername(), r.getStatus(),
          r.getIsFinished(), r.getTotalDistance());
    }

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    List<RunningResult> runningResults = new ArrayList<>();

    int savedCount = 0;

    // ✅ 모든 참가자 처리 (완주, 타임아웃, 포기 모두 저장)
    for (BattleRankingResDto ranking : rankings) {
      log.info("🔍 처리 중: userId={}, rank={}, status={}",
          ranking.getUserId(), ranking.getRank(), ranking.getStatus());

      User user = userRepository.findById(ranking.getUserId())
          .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

      // ✅ status 기반으로 RunStatus 결정
      RunStatus runStatus;
      if ("FINISHED".equals(ranking.getStatus())) {
        runStatus = RunStatus.COMPLETED;
      } else if ("TIMEOUT".equals(ranking.getStatus()) || "RUNNING".equals(ranking.getStatus())) {
        // ✅ RUNNING 상태도 타임아웃으로 처리 (Redis 업데이트 지연 대응)
        runStatus = RunStatus.TIME_OUT;
      } else {
        runStatus = RunStatus.GIVE_UP;  // GIVE_UP
      }

      RunningResult runningResult = RunningResult.builder()
          .user(user)
          .course(null)  // 배틀은 코스 없음
          .totalDistance(BigDecimal.valueOf(ranking.getTotalDistance() / 1000.0))  // km
          .totalTime(calculateTotalTime(ranking))
          .avgPace(parsePaceToBigDecimal(ranking.getCurrentPace()))
          .startedAt(startedAt)
          .runStatus(runStatus)
          .splitPace(new ArrayList<>())  // 구간 페이스 비어있음
          .runningType(RunningType.ONLINEBATTLE)
          .build();

      RunningResult saved = runningResultService.saveAndUpdateAverage(runningResult);
      runningResults.add(saved);

      savedCount++;
      log.info(
          "✅ RunningResult 저장 완료: userId={}, username={}, rank={}, status={}, runStatus={}, distance={}km",
          ranking.getUserId(), ranking.getUsername(), ranking.getRank(),
          ranking.getStatus(), runStatus, runningResult.getTotalDistance());
    }

    log.info("📊 저장 결과: 총={}명, 저장={}명", rankings.size(), savedCount);

    DistanceType distanceType = determineDistanceType(session.getTargetDistance());

    // ✅ 레이팅 정산은 완주자와 타임아웃만 (포기자 제외)
    List<RunningResult> ratedResults = runningResults.stream()
        .filter(
            r -> r.getRunStatus() == RunStatus.COMPLETED || r.getRunStatus() == RunStatus.TIME_OUT)
        .toList();

    List<RunningResult> giveUpResults = runningResults.stream()
        .filter(r -> r.getRunStatus() == RunStatus.GIVE_UP)
        .toList();

    // ✅ 레이팅 정산 (완주 + 타임아웃) - 여기서 BattleResult 저장됨
    if (!ratedResults.isEmpty()) {
      distanceRatingService.processBattleResults(sessionId, ratedResults, distanceType);
    }

    // ✅ 포기자 BattleResult 별도 저장 (패널티 포함)
    for (RunningResult giveUpResult : giveUpResults) {
      User user = giveUpResult.getUser();
      DistanceRating rating = distanceRatingRepository
          .findByUserIdAndDistanceType(user.getId(), distanceType)
          .orElseGet(() -> createNewRatingForGiveUp(user, distanceType));

      int previousRating = rating.getCurrentRating();

      // ✅ 포기자 패널티: 타임아웃보다 더 큰 패널티
      int penalty = calculateGiveUpPenalty(rating, distanceType);
      rating.updateRating(-penalty, 0);  // 음수로 점수 감소
      distanceRatingRepository.save(rating);

      BattleResult giveUpBattleResult = BattleResult.builder()
          .session(session)
          .user(user)
          .runningResult(giveUpResult)
          .distanceType(distanceType)
          .ranking(0)
          .previousRating(previousRating)
          .currentRating(rating.getCurrentRating())
          .build();

      battleResultRepository.save(giveUpBattleResult);

      log.info(" 포기자 BattleResult 저장 (패널티 -{}점): userId={}, username={}, rating: {} -> {}",
          penalty, user.getId(), user.getName(), previousRating, rating.getCurrentRating());
    }

    log.info(" 배틀 결과 저장 및 레이팅 정산 완료: sessionId={}, 레이팅대상={}명, 포기자={}명",
        sessionId, ratedResults.size(), giveUpResults.size());
  }

  /**
   * 총 시간 계산 (초)
   */
  private Integer calculateTotalTime(BattleRankingResDto ranking) {
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

    publishToRedis("/sub/battle/" + sessionId + "/complete", message);

    log.info("🏁 배틀 종료 메시지 전송: sessionId={}", sessionId);
  }

  /**
   * DB에서 세션 정보를 기반으로 순위 복구
   */
  private List<BattleRankingResDto> recoverRankingsFromDB(Long sessionId) {
    try {
      MatchSession session = matchSessionRepository.findById(sessionId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

      List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

      if (sessionUsers.isEmpty()) {
        log.warn(" DB에도 참가자 정보 없음: sessionId={}", sessionId);
        return Collections.emptyList();
      }

      log.info(" DB에서 참가자 복구: sessionId={}, 참가자={}명", sessionId, sessionUsers.size());

      List<BattleRankingResDto> recoveredRankings = new ArrayList<>();

      for (SessionUser su : sessionUsers) {
        User user = su.getUser();

        // RunningResult에서 이미 저장된 결과 조회 시도
        RunningResult existingResult = runningResultRepository
            .findByUserIdAndRunningTypeAndStartedAt(user.getId(), RunningType.ONLINEBATTLE,
                session.getCreatedAt())
            .orElse(null);

        if (existingResult != null) {
          // 이미 저장된 RunningResult가 있으면 그것을 기반으로 복구
          double distanceMeters = existingResult.getTotalDistance().doubleValue() * 1000;
          String status = existingResult.getRunStatus() == RunStatus.COMPLETED ? "FINISHED" :
              existingResult.getRunStatus() == RunStatus.TIME_OUT ? "TIMEOUT" : "GIVE_UP";

          Integer totalTime = existingResult.getTotalTime();
          Long finishTimeMillis = totalTime != null ? totalTime.longValue() * 1000L : null;
          LocalDateTime finishTimeActual = null;
          if (existingResult.getRunStatus() == RunStatus.COMPLETED
              && existingResult.getStartedAt() != null
              && totalTime != null) {
            finishTimeActual = existingResult.getStartedAt().plusSeconds(totalTime);
          }

          // 거리 타입 결정
          DistanceType distanceType = determineDistanceType(session.getTargetDistance());
          double targetDistanceMeters = session.getTargetDistance() * 1000.0;
          double remainingDistance = Math.max(0, targetDistanceMeters - distanceMeters);
          double progressPercent =
              targetDistanceMeters > 0 ? (distanceMeters / targetDistanceMeters) * 100 : 0.0;

          BattleRankingResDto dto = BattleRankingResDto.builder()
              .userId(user.getId())
              .username(user.getName())
              .totalDistance(distanceMeters)
              .remainingDistance(remainingDistance)
              .progressPercent(progressPercent)
              .status(status)
              .isFinished(existingResult.getRunStatus() == RunStatus.COMPLETED)
              .currentPace(
                  existingResult.getAvgPace() != null
                      ? String.format(
                      "%d:%02d",
                      existingResult.getAvgPace().intValue(),
                      (int) (
                          (existingResult.getAvgPace().doubleValue()
                              - existingResult.getAvgPace().intValue()) * 60
                      )
                  )
                      : "0:00"
              )
              .finishTime(finishTimeMillis)
              .finishTimeActual(finishTimeActual)
              .rank(0)  // 임시 순위
              .build();
          recoveredRankings.add(dto);
        } else {
          // RunningResult가 없으면 포기자로 간주
          DistanceType distanceType = determineDistanceType(session.getTargetDistance());
          double targetDistanceMeters = session.getTargetDistance() * 1000.0;

          BattleRankingResDto dto = BattleRankingResDto.builder()
              .userId(user.getId())
              .username(user.getName())
              .totalDistance(0.0)
              .remainingDistance(targetDistanceMeters)
              .progressPercent(0.0)
              .status("GIVE_UP")
              .isFinished(false)
              .currentPace("0:00")
              .rank(0)
              .build();
          recoveredRankings.add(dto);
        }
      }

      // 거리 기준으로 정렬하여 순위 부여
      recoveredRankings.sort((a, b) -> Double.compare(b.getTotalDistance(), a.getTotalDistance()));
      for (int i = 0; i < recoveredRankings.size(); i++) {
        BattleRankingResDto dto = recoveredRankings.get(i);
        if (!"GIVE_UP".equals(dto.getStatus())) {
          dto.setRank(i + 1);
        }
      }

      log.info(" DB 복구 완료: sessionId={}, 복구된 참가자={}명", sessionId, recoveredRankings.size());
      return recoveredRankings;

    } catch (Exception e) {
      log.error(" DB 복구 실패: sessionId={}", sessionId, e);
      return Collections.emptyList();
    }
  }

  /**
   * 최소한의 배틀 결과 저장 (Redis/DB 복구 모두 실패한 경우)
   */
  private void saveMinimalBattleResults(Long sessionId, LocalDateTime startedAt) {
    try {
      MatchSession session = matchSessionRepository.findById(sessionId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

      List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

      if (sessionUsers.isEmpty()) {
        log.error(" 참가자 정보도 없음 - 저장 불가: sessionId={}", sessionId);
        return;
      }

      DistanceType distanceType = determineDistanceType(session.getTargetDistance());

      for (SessionUser su : sessionUsers) {
        User user = su.getUser();

        // 최소한의 RunningResult 저장
        RunningResult minimalResult = RunningResult.builder()
            .user(user)
            .course(null)
            .totalDistance(BigDecimal.ZERO)
            .totalTime(0)
            .avgPace(BigDecimal.ZERO)
            .startedAt(startedAt)
            .runStatus(RunStatus.GIVE_UP)
            .splitPace(new ArrayList<>())
            .runningType(RunningType.ONLINEBATTLE)
            .build();

        runningResultRepository.save(minimalResult);

        // 최소한의 BattleResult 저장
        DistanceRating rating = distanceRatingRepository
            .findByUserIdAndDistanceType(user.getId(), distanceType)
            .orElse(null);
        int currentRatingValue = rating != null ? rating.getCurrentRating() : 0;

        BattleResult minimalBattleResult = BattleResult.builder()
            .session(session)
            .user(user)
            .runningResult(minimalResult)
            .distanceType(distanceType)
            .ranking(0)
            .previousRating(currentRatingValue)
            .currentRating(currentRatingValue)
            .build();

        battleResultRepository.save(minimalBattleResult);

        log.warn(" 최소한 데이터 저장: sessionId={}, userId={}, username={}",
            sessionId, user.getId(), user.getName());
      }

      log.warn(" 최소한의 데이터 저장 완료: sessionId={}, 참가자={}명",
          sessionId, sessionUsers.size());

    } catch (Exception e) {
      log.error(" 최소한 데이터 저장도 실패: sessionId={}", sessionId, e);
    }
  }

  /**
   * 포기자 패널티 계산
   */
  private int calculateGiveUpPenalty(DistanceRating rating, DistanceType distanceType) {
    int k = kFactor(rating.getUser(), distanceType);

    return Math.max(10, (int) Math.round(k * 0.25));
  }

  /**
   * K-factor 계산 (DistanceRatingService와 동일한 로직)
   */
  private int kFactor(User user, DistanceType distanceType) {
    long games = battleResultRepository.countByUserAndDistanceType(user, distanceType);
    if (games < 10) {
      return 40;
    }
    if (games < 30) {
      return 32;
    }
    return 24;
  }

  /**
   * 포기자를 위한 새 레이팅 생성
   */
  private DistanceRating createNewRatingForGiveUp(User user, DistanceType type) {
    return distanceRatingRepository.findByUserIdAndDistanceType(user.getId(), type)
        .orElseGet(() -> distanceRatingRepository.save(
            DistanceRating.builder()
                .user(user)
                .distanceType(type)
                .build()
        ));
  }


  /**
   * 배틀 결과 조회
   */
  public Map<String, Object> getBattleResult(Long sessionId, Long userId) {
    // 세션 조회
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 순위 조회
    List<BattleRankingResDto> rankings = getRankings(sessionId);

    // 내 데이터 찾기
    BattleRankingResDto myData = rankings.stream()
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
  private Long calculateFinishTime(BattleRankingResDto ranking) {
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
    // ✅ 완주 처리 (중복 호출은 idempotent하므로 문제없음)
    battleRedisService.finishUser(sessionId, userId);
    log.info("🏆 참가자 완주 처리 (REST API): sessionId={}, userId={}", sessionId, userId);

    // ✅ 모든 참가자 완주 확인
    checkAndFinishBattle(sessionId);
  }

  /**
   * 참가자 포기 처리
   */
  @Transactional
  public Map<String, Object> quitBattle(Long sessionId, Long userId) {
    Map<String, Object> result = new HashMap<>();

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // ✅ 이미 종료된 배틀이면 결과 메시지만 재전송
    if (session.getStatus() == SessionStatus.COMPLETED) {
      log.info("ℹ️ 이미 종료된 배틀 - 결과 메시지 재전송: sessionId={}, userId={}",
          sessionId, userId);
      sendBattleCompleteMessage(sessionId);
      result.put("shouldShowResult", true);
      result.put("message", "이미 종료된 배틀입니다.");
      return result;
    }

    SessionUser sessionUser = sessionUserRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

    User user = sessionUser.getUser();

    // 1. Redis에서 현재 데이터 조회 후 GIVE_UP 상태로 변경
    battleRedisService.setUserGiveUp(sessionId, userId);  // ✅ 포기 상태로 변경

    log.info("🚪 포기자 상태 변경: sessionId={}, userId={}", sessionId, userId);

    // 2. SessionUser soft delete
    sessionUser.delete();
    sessionUserRepository.save(sessionUser);

    // 3. 남은 참가자 확인
    List<SessionUser> remainingUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

    if (remainingUsers.isEmpty()) {
      // 모두 포기 - 배틀 종료 (✅ finishBattle 호출)
      log.info("⚠️ 모든 참가자 포기 - 배틀 종료: sessionId={}", sessionId);
      finishBattle(sessionId);  // ✅ saveBattleResults 호출됨

      result.put("shouldShowResult", true);
      result.put("message", "모든 참가자가 포기하여 배틀이 종료되었습니다.");

    } else {
      // 1명 이상 남음 - 계속 진행 (완주할 때까지)
      log.info("✅ 남은 참가자 {}명 - 계속 진행: sessionId={}",
          remainingUsers.size(), sessionId);

      // 포기 알림 메시지 전송
      sendQuitMessage(sessionId, user.getName(), remainingUsers.size());

      result.put("shouldShowResult", false);
      result.put("message", "포기 처리가 완료되었습니다.");
    }

    return result;
  }

  /**
   * 포기 알림 메시지 전송
   */
  private void sendQuitMessage(Long sessionId, String quitUserName, int remainingCount) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "USER_QUIT");
    message.put("message", quitUserName + "님이 포기하셨습니다. (남은 인원: " + remainingCount + "명)");
    message.put("quitUserName", quitUserName);
    message.put("remainingCount", remainingCount);
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/quit", message);

    log.info("📤 포기 메시지 발행: sessionId={}, 남은 인원={}명",
        sessionId, remainingCount);
  }

  /**
   * ✅ 타임아웃 만료 시 배틀 종료 처리 (스케줄러에서 호출)
   */
  @Transactional
  public void executeTimeoutFinish(Long sessionId) {
    log.info("🔥 타임아웃 종료 처리 시작: sessionId={}", sessionId);

    // 세션 상태 확인
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 이미 종료된 경우 스킵
    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
      log.info("ℹ️ 이미 종료된 배틀: sessionId={}, status={}", sessionId, session.getStatus());
      return;
    }

    // 현재 순위 조회
    List<BattleRankingResDto> rankings = getRankings(sessionId);

    // 미완주자 수 확인
    long notFinishedCount = rankings.stream()
        .filter(r -> !r.getIsFinished())
        .count();

    log.info("👥 타임아웃 만료 - 미완주자: {}명 (자동 리타이어 처리)", notFinishedCount);

    // 미완주자 로깅
    rankings.stream()
        .filter(r -> !r.getIsFinished())
        .forEach(p -> log.info("  - 미완주: userId={}, username={}, distance={}m",
            p.getUserId(), p.getUsername(), p.getTotalDistance()));

    // ✅ 모든 미완주자를 Redis에서 TIMEOUT 상태로 변경
    battleRedisService.setAllUnfinishedToTimeout(sessionId);

    // ✅ Redis 업데이트 완료 대기 (200ms)
    try {
      log.info("⏳ Redis 업데이트 반영 대기 중... (200ms)");
      Thread.sleep(200);
    } catch (InterruptedException e) {
      log.warn("⚠️ 대기 중 인터럽트 발생", e);
      Thread.currentThread().interrupt();
    }

    // ✅ 업데이트 확인용 재조회
    List<BattleRankingResDto> updatedRankings = getRankings(sessionId);
    log.info("📊 업데이트 후 재조회 ({}명):", updatedRankings.size());
    for (BattleRankingResDto r : updatedRankings) {
      log.info("  - rank={}, userId={}, status={}, finished={}",
          r.getRank(), r.getUserId(), r.getStatus(), r.getIsFinished());
    }

    // 배틀 종료 (미완주자는 TIME_OUT으로 저장)
    finishBattle(sessionId, true);  // ✅ 타임아웃 플래그 전달

    log.info("✅ 타임아웃 종료 완료: sessionId={}", sessionId);
  }

  /**
   * ✅ 타임아웃 시작 메시지 전송 (클라이언트용)
   */
  private void sendTimeoutStartMessage(Long sessionId, Integer timeoutSeconds) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BATTLE_TIMEOUT_START");
    message.put("timeoutSeconds", timeoutSeconds);
    message.put("message", "🏆 1등 완주! " + timeoutSeconds + "초 내 완주하세요!");
    message.put("timestamp", LocalDateTime.now());

    publishToRedis("/sub/battle/" + sessionId + "/timeout-start", message);

    log.info("⏰ 타임아웃 시작 메시지 전송: sessionId={}, timeout={}초",
        sessionId, timeoutSeconds);
  }
}

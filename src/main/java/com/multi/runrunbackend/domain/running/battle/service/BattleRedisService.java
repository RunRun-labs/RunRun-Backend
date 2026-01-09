package com.multi.runrunbackend.domain.running.battle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.running.battle.dto.BattleUserDto;
import com.multi.runrunbackend.domain.running.battle.dto.TimeoutDto;
import com.multi.runrunbackend.domain.running.battle.dto.req.BattleGpsReqDto.GpsData;
import com.multi.runrunbackend.domain.running.battle.dto.res.BattleRankingResDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/**
 * @author : chang
 * @description : 온라인 배틀 Redis 데이터 처리 서비스
 * @filename : BattleRedisService
 * @since : 2025-12-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BattleRedisService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String BATTLE_USER_KEY = "battle:%d:user:%d";
  private static final String BATTLE_RANKING_KEY = "battle:%d:ranking";
  private static final String BATTLE_TIMEOUT_KEY = "battle:%d:timeout";  // ✅ 타임아웃 정보
  private static final Duration BATTLE_TTL = Duration.ofHours(3);

  /**
   * 배틀 참가자 초기화
   */
  public void initializeBattleUser(Long sessionId, Long userId, String username) {
    String key = String.format(BATTLE_USER_KEY, sessionId, userId);

    LocalDateTime now = LocalDateTime.now();

    BattleUserDto userData = BattleUserDto.builder()
        .userId(userId)
        .username(username)
        .totalDistance(0.0)
        .currentSpeed(0.0)
        .currentPace("0:00")
        .lastGpsLat(null)
        .lastGpsLng(null)
        .lastGpsTime(null)
        .startTime(now)  // ✅ 시작 시간 설정
        .isFinished(false)
        .finishTime(null)
        .status("RUNNING")  // ✅ 초기 상태
        .build();

    try {
      String json = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, json, BATTLE_TTL);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 직렬화 실패: sessionId={}, userId={}", sessionId, userId, e);
      throw new RuntimeException("배틀 참가자 초기화 실패", e);
    }

    String rankingKey = String.format(BATTLE_RANKING_KEY, sessionId);
    redisTemplate.opsForZSet().add(rankingKey, userId.toString(), 0.0);
    redisTemplate.expire(rankingKey, BATTLE_TTL);

    log.info("✅ 배틀 참가자 초기화: sessionId={}, userId={}, username={}, startTime={}",
        sessionId, userId, username, now);
  }

  /**
   * GPS 데이터 업데이트
   */
  public void updateGpsData(Long sessionId, Long userId, GpsData gps, Double totalDistance) {
    String key = String.format(BATTLE_USER_KEY, sessionId, userId);

    String json = (String) redisTemplate.opsForValue().get(key);
    if (json == null) {
      log.warn("⚠️ 배틀 참가자 데이터 없음: sessionId={}, userId={}", sessionId, userId);
      return;
    }

    try {
      BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);

      userData.setTotalDistance(totalDistance);
      userData.setCurrentSpeed(gps.getSpeed() != null ? gps.getSpeed() : 0.0);
      userData.setLastGpsLat(gps.getLat());
      userData.setLastGpsLng(gps.getLng());
      userData.setLastGpsTime(LocalDateTime.now());

      // ✅ 페이스 계산: 완주한 사람은 페이스 고정, 미완주자만 실시간 업데이트
      if (!userData.getIsFinished()) {  // ✅ 완주 안 한 사람만 페이스 재계산
        if (totalDistance > 0 && userData.getStartTime() != null) {
          // 경과 시간 (초)
          long elapsedSeconds = Duration.between(userData.getStartTime(), LocalDateTime.now())
              .getSeconds();

          if (elapsedSeconds > 0) {
            // 평균 속도 (m/s) = 총거리 / 경과시간
            double avgSpeed = totalDistance / elapsedSeconds;

            // 페이스 (min/km) = 1000m / avgSpeed(m/s) / 60s
            double paceMinutesDecimal = 1000.0 / avgSpeed / 60.0;
            int minutes = (int) paceMinutesDecimal;
            int seconds = (int) ((paceMinutesDecimal - minutes) * 60);

            userData.setCurrentPace(String.format("%d:%02d", minutes, seconds));

            log.info(
                "📊 페이스 계산: sessionId={}, userId={}, distance={}m, elapsed={}s, avgSpeed={:.2f}m/s, pace={}",
                sessionId, userId, totalDistance, elapsedSeconds, avgSpeed,
                userData.getCurrentPace());
          } else {
            userData.setCurrentPace("0:00");
          }
        } else {
          userData.setCurrentPace("0:00");
        }
      } else {
        log.trace("🏁 완주자는 페이스 고정: sessionId={}, userId={}, pace={}",
            sessionId, userId, userData.getCurrentPace());
      }

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      String rankingKey = String.format(BATTLE_RANKING_KEY, sessionId);
      redisTemplate.opsForZSet().add(rankingKey, userId.toString(), totalDistance);

      log.trace("📍 GPS 업데이트: sessionId={}, userId={}, distance={}m",
          sessionId, userId, totalDistance);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }

  /**
   * 전체 순위 조회
   */
  public List<BattleRankingResDto> getRankings(Long sessionId, Double targetDistance) {
    String rankingKey = String.format(BATTLE_RANKING_KEY, sessionId);

    Set<ZSetOperations.TypedTuple<Object>> rankingSet =
        redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, -1);

    log.info("🔥🔥🔥 getRankings 호출: sessionId={}, ranking ZSet 크기={}", 
        sessionId, rankingSet == null ? 0 : rankingSet.size());

    if (rankingSet == null || rankingSet.isEmpty()) {
      log.warn("⚠️ 순위 데이터 없음: sessionId={}", sessionId);
      return new ArrayList<>();
    }

    log.info("🔥 Ranking ZSet 내용:");
    for (ZSetOperations.TypedTuple<Object> tuple : rankingSet) {
      log.info("  - userId={}, distance={}", tuple.getValue(), tuple.getScore());
    }

    List<BattleRankingResDto> rankings = new ArrayList<>();

    // ✅ 1단계: 모든 참가자 데이터 수집
    int nullJsonCount = 0;
    for (ZSetOperations.TypedTuple<Object> tuple : rankingSet) {
      Long userId = Long.parseLong(tuple.getValue().toString());
      Double distance = tuple.getScore();

      String userKey = String.format(BATTLE_USER_KEY, sessionId, userId);
      String json = (String) redisTemplate.opsForValue().get(userKey);

      log.info("🔥 userId={} 데이터 조회: json {}", userId, json == null ? "NULL" : "EXISTS");

      if (json != null) {
        try {
          BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);

          // ✅ finishTime 계산
          Long finishTimeMillis = null;
          if (userData.getIsFinished() && userData.getFinishTime() != null
              && userData.getStartTime() != null) {
            // 실제 완주 시간 = finishTime - startTime
            finishTimeMillis = Duration.between(userData.getStartTime(), userData.getFinishTime())
                .toMillis();
          } else if (userData.getStartTime() != null) {
            // 아직 완주 안 한 경우 = 현재까지의 경과 시간
            finishTimeMillis = Duration.between(userData.getStartTime(), LocalDateTime.now())
                .toMillis();
          } else {
            // startTime이 없으면 0
            finishTimeMillis = 0L;
          }

          // ✅ status 결정 로직 (null 처리)
          String status;
          if (userData.getStatus() != null) {
            status = userData.getStatus();
          } else {
            // 기존 데이터는 status가 없으므로 isFinished로 판단
            status = userData.getIsFinished() ? "FINISHED" : "RUNNING";
          }

          rankings.add(BattleRankingResDto.builder()
              .rank(0)  // 임시 순위 (정렬 후 부여)
              .userId(userId)
              .username(userData.getUsername())
              .profileImage(null)
              .totalDistance(distance)
              .remainingDistance(Math.max(0, targetDistance - distance))
              .progressPercent((distance / targetDistance) * 100)
              .currentPace(userData.getCurrentPace())
              .isFinished(userData.getIsFinished())
              .finishTime(finishTimeMillis)  // 경과 시간 (표시용)
              .finishTimeActual(userData.getFinishTime())  // ✅ 실제 완주 시각 (순위 비교용)
              .status(status)
              .build());
        } catch (JsonProcessingException e) {
          log.error("❌ JSON 역직렬화 실패: sessionId={}, userId={}", sessionId, userId, e);
        }
      } else {
        nullJsonCount++;
        log.error("❌❌❌ userId={} 데이터가 Redis에 없음! (ranking에는 있지만 user 데이터 없음)", userId);
      }
    }

    log.info("🔥🔥🔥 데이터 수집 완료: 전체={}명, 조회성공={}명, NULL={}명", 
        rankingSet.size(), rankings.size(), nullJsonCount);

    // ✅ 2단계: 상태별로 분류 및 정렬
    // 완주자: FINISHED 상태, 완주 시각순 정렬
    List<BattleRankingResDto> finishedRankings = rankings.stream()
        .filter(r -> "FINISHED".equals(r.getStatus()))
        .sorted((a, b) -> {
          // ✅ 실제 완주 시각으로 비교 (빠른 시각이 1등)
          if (a.getFinishTimeActual() == null && b.getFinishTimeActual() == null) {
            return 0;
          }
          if (a.getFinishTimeActual() == null) {
            return 1;  // null은 뒤로
          }
          if (b.getFinishTimeActual() == null) {
            return -1;
          }
          return a.getFinishTimeActual().compareTo(b.getFinishTimeActual());  // 오름차순
        })
        .collect(java.util.stream.Collectors.toList());

    // ✅ 타임아웃자: TIMEOUT 또는 RUNNING 상태, 거리 내림차순 정렬
    List<BattleRankingResDto> timeoutRankings = rankings.stream()
        .filter(r -> "TIMEOUT".equals(r.getStatus()) || "RUNNING".equals(r.getStatus()))
        .sorted((a, b) -> Double.compare(b.getTotalDistance(), a.getTotalDistance()))
        .collect(java.util.stream.Collectors.toList());

    // ✅ 포기자: GIVE_UP 상태만 (순위 없음)
    List<BattleRankingResDto> giveUpRankings = rankings.stream()
        .filter(r -> "GIVE_UP".equals(r.getStatus()))
        .collect(java.util.stream.Collectors.toList());

    // ✅ 3단계: 순위 부여
    // 완주자: 1, 2, 3...
    for (int i = 0; i < finishedRankings.size(); i++) {
      BattleRankingResDto ranking = finishedRankings.get(i);
      ranking.setRank(i + 1);

      log.info("🏆 {}등 (완주): userId={}, username={}, 실제완주시각={}",
          ranking.getRank(), ranking.getUserId(), ranking.getUsername(),
          ranking.getFinishTimeActual());
    }

    // 타임아웃자: (완주자 수 + 1)부터
    int nextRank = finishedRankings.size() + 1;
    for (int i = 0; i < timeoutRankings.size(); i++) {
      BattleRankingResDto ranking = timeoutRankings.get(i);
      ranking.setRank(nextRank++);

      log.info("⏰ {}등 (타임아웃): userId={}, username={}, distance={}m",
          ranking.getRank(), ranking.getUserId(), ranking.getUsername(),
          ranking.getTotalDistance());
    }

    // 포기자: 순위 0
    for (BattleRankingResDto giveUp : giveUpRankings) {
      giveUp.setRank(0);
      log.info("🚪 순위없음 (포기): userId={}, username={}",
          giveUp.getUserId(), giveUp.getUsername());
    }

    // ✅ 4단계: 합치기 (완주자 + 타임아웃자 + 포기자)
    List<BattleRankingResDto> finalRankings = new ArrayList<>();
    finalRankings.addAll(finishedRankings);
    finalRankings.addAll(timeoutRankings);
    finalRankings.addAll(giveUpRankings);

    log.info("📊 순위 조회 완료: sessionId={}, 완주={}명, 타임아웃={}명, 포기={}명",
        sessionId, finishedRankings.size(), timeoutRankings.size(), giveUpRankings.size());
    return finalRankings;
  }

  /**
   * 참가자 완주 처리
   */
  public void finishUser(Long sessionId, Long userId) {
    String key = String.format(BATTLE_USER_KEY, sessionId, userId);

    String json = (String) redisTemplate.opsForValue().get(key);
    if (json == null) {
      log.warn("⚠️ 배틀 참가자 데이터 없음: sessionId={}, userId={}", sessionId, userId);
      return;
    }

    try {
      BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);

      // ✅ 이미 완주한 경우 더 이상 처리하지 않음 (멱등성 보장)
      if (userData.getIsFinished()) {
        log.warn("⚠️⚠️⚠️ 이미 완주 처리된 참가자: sessionId={}, userId={}, 기존완주시각={}",
            sessionId, userId, userData.getFinishTime());
        return;  // ✅ 중복 완주 방지!
      }

      LocalDateTime finishTime = LocalDateTime.now();

      userData.setIsFinished(true);
      userData.setFinishTime(finishTime);
      userData.setStatus("FINISHED");  // ✅ 상태 업데이트

      // ✅ 완주 시점의 최종 페이스 계산 (이후 고정됨)
      if (userData.getTotalDistance() > 0 && userData.getStartTime() != null) {
        long elapsedSeconds = Duration.between(userData.getStartTime(), finishTime).getSeconds();

        if (elapsedSeconds > 0) {
          double avgSpeed = userData.getTotalDistance() / elapsedSeconds;
          double paceMinutesDecimal = 1000.0 / avgSpeed / 60.0;
          int minutes = (int) paceMinutesDecimal;
          int seconds = (int) ((paceMinutesDecimal - minutes) * 60);

          userData.setCurrentPace(String.format("%d:%02d", minutes, seconds));

          log.info("🏁 완주 페이스 계산: sessionId={}, userId={}, distance={}m, elapsed={}s, pace={}",
              sessionId, userId, userData.getTotalDistance(), elapsedSeconds,
              userData.getCurrentPace());
        }
      }

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      log.info(
          "🏁🏁🏁 참가자 완주: sessionId={}, userId={}, username={}, 실제완주시각={}, distance={}m, pace={}",
          sessionId, userId, userData.getUsername(), finishTime, userData.getTotalDistance(),
          userData.getCurrentPace());
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }

  /**
   * 참가자 타임아웃 처리
   */
  public void setUserTimeout(Long sessionId, Long userId) {
    String key = String.format(BATTLE_USER_KEY, sessionId, userId);

    String json = (String) redisTemplate.opsForValue().get(key);
    if (json == null) {
      log.warn("⚠️ 배틀 참가자 데이터 없음: sessionId={}, userId={}", sessionId, userId);
      return;
    }

    try {
      BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);
      userData.setStatus("TIMEOUT");  // ✅ 타임아웃 상태

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      log.info("⏰ 참가자 타임아웃: sessionId={}, userId={}", sessionId, userId);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }

  /**
   * 모든 미완주자를 타임아웃으로 표시
   */
  public void setAllUnfinishedToTimeout(Long sessionId) {
    String rankingKey = String.format(BATTLE_RANKING_KEY, sessionId);
    Set<ZSetOperations.TypedTuple<Object>> rankingSet =
        redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, -1);

    if (rankingSet == null || rankingSet.isEmpty()) {
      log.warn("⚠️ 랭킹 데이터 없음: sessionId={}", sessionId);
      return;
    }

    log.info("🔍 타임아웃 처리 시작: sessionId={}, 전체={}명", sessionId, rankingSet.size());

    int timeoutCount = 0;
    for (ZSetOperations.TypedTuple<Object> tuple : rankingSet) {
      Long userId = Long.parseLong(tuple.getValue().toString());
      String userKey = String.format(BATTLE_USER_KEY, sessionId, userId);
      String json = (String) redisTemplate.opsForValue().get(userKey);

      if (json != null) {
        try {
          BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);

          log.info("🔍 처리 전: userId={}, isFinished={}, status={}, distance={}m", 
              userId, userData.getIsFinished(), userData.getStatus(), userData.getTotalDistance());

          // ✅ 미완주자만 타임아웃으로 변경 (상태가 null이거나 RUNNING인 경우)
          if (!userData.getIsFinished() && (userData.getStatus() == null || "RUNNING".equals(userData.getStatus()))) {
            userData.setStatus("TIMEOUT");
            String updatedJson = objectMapper.writeValueAsString(userData);
            redisTemplate.opsForValue().set(userKey, updatedJson, BATTLE_TTL);

            timeoutCount++;
            log.info("⏰ 자동 타임아웃 설정: sessionId={}, userId={}, distance={}m", 
                sessionId, userId, userData.getTotalDistance());
          } else {
            log.info("ℹ️ 타임아웃 대상 아님: userId={}, isFinished={}, status={}", 
                userId, userData.getIsFinished(), userData.getStatus());
          }
        } catch (JsonProcessingException e) {
          log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
        }
      } else {
        log.warn("⚠️ 사용자 데이터 없음: sessionId={}, userId={}", sessionId, userId);
      }
    }

    log.info("✅ 모든 미완주자 타임아웃 처리 완료: sessionId={}, 타임아웃={}명", sessionId, timeoutCount);
  }

  /**
   * ✅ 참가자 포기 처리 (상태만 변경, 데이터는 유지)
   */
  public void setUserGiveUp(Long sessionId, Long userId) {
    String key = String.format(BATTLE_USER_KEY, sessionId, userId);

    String json = (String) redisTemplate.opsForValue().get(key);
    if (json == null) {
      log.warn("⚠️ 배틀 참가자 데이터 없음: sessionId={}, userId={}", sessionId, userId);
      return;
    }

    try {
      BattleUserDto userData = objectMapper.readValue(json, BattleUserDto.class);
      userData.setStatus("GIVE_UP");  // ✅ 포기 상태로 변경

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      log.info("🚺 참가자 포기 처리: sessionId={}, userId={}, distance={}m", 
          sessionId, userId, userData.getTotalDistance());
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }

  /**
   * ❌ 참가자 제거 (사용하지 않음 - 포기자도 결과 저장 필요)
   * @deprecated setUserGiveUp 사용 권장
   */
  @Deprecated
  public void removeUser(Long sessionId, Long userId) {
    // 1. 사용자 데이터 삭제
    String userKey = String.format(BATTLE_USER_KEY, sessionId, userId);
    redisTemplate.delete(userKey);

    // 2. 랭킹에서 제거
    String rankingKey = String.format(BATTLE_RANKING_KEY, sessionId);
    redisTemplate.opsForZSet().remove(rankingKey, userId.toString());

    log.info("🗑️ Redis 제거 완료: sessionId={}, userId={}", sessionId, userId);
  }

  /**
   * 타임아웃 정보 설정 (첫 완주자 발생 시)
   */
  public void setFirstFinishTime(Long sessionId, Integer timeoutSeconds) {
    String key = String.format(BATTLE_TIMEOUT_KEY, sessionId);

    TimeoutDto timeoutData = TimeoutDto.builder()
        .firstFinishTime(LocalDateTime.now())
        .timeoutSeconds(timeoutSeconds)
        .isTimerStarted(true)
        .build();

    try {
      String json = objectMapper.writeValueAsString(timeoutData);
      redisTemplate.opsForValue().set(key, json, BATTLE_TTL);

      log.info("⏰ 타임아웃 시작: sessionId={}, timeout={}초, startTime={}",
          sessionId, timeoutSeconds, timeoutData.getFirstFinishTime());
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 직렬화 실패: sessionId={}", sessionId, e);
      throw new RuntimeException("타임아웃 정보 저장 실패", e);
    }
  }

  /**
   * 타임아웃 정보 조회
   */
  public TimeoutDto getTimeoutData(Long sessionId) {
    String key = String.format(BATTLE_TIMEOUT_KEY, sessionId);
    String json = (String) redisTemplate.opsForValue().get(key);

    if (json == null) {
      return null;
    }

    try {
      return objectMapper.readValue(json, TimeoutDto.class);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 역직렬화 실패: sessionId={}", sessionId, e);
      return null;
    }
  }
}

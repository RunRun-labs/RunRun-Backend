package com.multi.runrunbackend.domain.running.battle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.running.battle.dto.BattleUserDto;
import com.multi.runrunbackend.domain.running.battle.dto.TimeoutData;
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

    if (rankingSet == null || rankingSet.isEmpty()) {
      log.warn("⚠️ 순위 데이터 없음: sessionId={}", sessionId);
      return new ArrayList<>();
    }

    List<BattleRankingResDto> rankings = new ArrayList<>();

    // ✅ 1단계: 모든 참가자 데이터 수집
    for (ZSetOperations.TypedTuple<Object> tuple : rankingSet) {
      Long userId = Long.parseLong(tuple.getValue().toString());
      Double distance = tuple.getScore();

      String userKey = String.format(BATTLE_USER_KEY, sessionId, userId);
      String json = (String) redisTemplate.opsForValue().get(userKey);

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
              .build());
        } catch (JsonProcessingException e) {
          log.error("❌ JSON 역직렬화 실패: sessionId={}, userId={}", sessionId, userId, e);
        }
      }
    }

    // ✅ 2단계: 정렬 (완주자만) - ✅ 실제 완주 시각으로 비교!
    List<BattleRankingResDto> finishedRankings = rankings.stream()
        .filter(BattleRankingResDto::getIsFinished)
        .sorted((a, b) -> {
          // ✅ 실제 완주 시각으로 비교 (빠른 시각이 1등)
          if (a.getFinishTimeActual() == null && b.getFinishTimeActual() == null) return 0;
          if (a.getFinishTimeActual() == null) return 1;  // null은 뒤로
          if (b.getFinishTimeActual() == null) return -1;
          return a.getFinishTimeActual().compareTo(b.getFinishTimeActual());  // 오름차순
        })
        .collect(java.util.stream.Collectors.toList());

    // 미완주자 리스트 (순위 없음)
    List<BattleRankingResDto> unfinishedRankings = rankings.stream()
        .filter(r -> !r.getIsFinished())
        .sorted((a, b) -> Double.compare(b.getTotalDistance(), a.getTotalDistance()))  // 거리 내림차순
        .collect(java.util.stream.Collectors.toList());

    // ✅ 3단계: 완주자만 순위 부여 (1, 2, 3...)
    for (int i = 0; i < finishedRankings.size(); i++) {
      BattleRankingResDto ranking = finishedRankings.get(i);
      ranking.setRank(i + 1);
      
      // ✅ 순위 부여 로그 (실제 완주 시각 포함)
      log.info("🏆 {}\ub4f1: userId={}, username={}, 실제완주시각={}, 경과시간={}ms",
          ranking.getRank(), ranking.getUserId(), ranking.getUsername(),
          ranking.getFinishTimeActual(), ranking.getFinishTime());
    }

    // ✅ 미완주자는 순위 0 (완주 실패)
    for (BattleRankingResDto unfinished : unfinishedRankings) {
      unfinished.setRank(0);  // 0 = 완주 실패
    }

    // ✅ 4단계: 완주자 + 미완주자 합치기
    List<BattleRankingResDto> finalRankings = new ArrayList<>();
    finalRankings.addAll(finishedRankings);
    finalRankings.addAll(unfinishedRankings);

    log.info("📊 순위 조회 완료: sessionId={}, 완주자={}명, 미완주자={}명", 
        sessionId, finishedRankings.size(), unfinishedRankings.size());
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
              sessionId, userId, userData.getTotalDistance(), elapsedSeconds, userData.getCurrentPace());
        }
      }

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      log.info("🏁🏁🏁 참가자 완주: sessionId={}, userId={}, username={}, 실제완주시각={}, distance={}m, pace={}",
          sessionId, userId, userData.getUsername(), finishTime, userData.getTotalDistance(), userData.getCurrentPace());
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }

  /**
   * 참가자 제거 (포기 시 사용)
   */
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

    TimeoutData timeoutData = TimeoutData.builder()
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
  public TimeoutData getTimeoutData(Long sessionId) {
    String key = String.format(BATTLE_TIMEOUT_KEY, sessionId);
    String json = (String) redisTemplate.opsForValue().get(key);

    if (json == null) {
      return null;
    }

    try {
      return objectMapper.readValue(json, TimeoutData.class);
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 역직렬화 실패: sessionId={}", sessionId, e);
      return null;
    }
  }
}

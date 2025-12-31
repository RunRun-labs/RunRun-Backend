package com.multi.runrunbackend.domain.running.battle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.running.battle.dto.BattleUserDto;
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

      // ✅ 페이스 계산: 총 거리 / 경과 시간으로 계산
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
              .finishTime(finishTimeMillis)
              .build());
        } catch (JsonProcessingException e) {
          log.error("❌ JSON 역직렬화 실패: sessionId={}, userId={}", sessionId, userId, e);
        }
      }
    }

    // ✅ 2단계: 정렬
    rankings.sort((a, b) -> {
      // 완주한 사람끼리 비교
      if (a.getIsFinished() && b.getIsFinished()) {
        // finishTime 오름차순 (빠른 사람이 1등)
        return Long.compare(a.getFinishTime(), b.getFinishTime());
      }
      // 완주한 사람이 안 한 사람보다 항상 앞
      else if (a.getIsFinished()) {
        return -1;
      } else if (b.getIsFinished()) {
        return 1;
      }
      // 둘 다 안 완주했으면 거리 내림차순 (멀리 간 사람이 앜)
      else {
        return Double.compare(b.getTotalDistance(), a.getTotalDistance());
      }
    });

    // ✅ 3단계: 순위 부여
    for (int i = 0; i < rankings.size(); i++) {
      rankings.get(i).setRank(i + 1);
    }

    log.info("📊 순위 조회 및 정렬 완료: sessionId={}, 참가자={}명", sessionId, rankings.size());
    return rankings;
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
      userData.setIsFinished(true);
      userData.setFinishTime(LocalDateTime.now());

      String updatedJson = objectMapper.writeValueAsString(userData);
      redisTemplate.opsForValue().set(key, updatedJson, BATTLE_TTL);

      log.info("🏁 참가자 완주: sessionId={}, userId={}, distance={}m",
          sessionId, userId, userData.getTotalDistance());
    } catch (JsonProcessingException e) {
      log.error("❌ JSON 처리 실패: sessionId={}, userId={}", sessionId, userId, e);
    }
  }


}

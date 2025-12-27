package com.multi.runrunbackend.domain.match.scheduler;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : MatchingScheduduler
 * @since : 2025-12-27 토요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingScheduler {

  private final RedisTemplate<String, String> redisTemplate;
  private final MatchSessionService matchSessionService;

  private static final String QUEUE_KEY_PREFIX = "matching_queue:";
  private static final String USER_STATUS_KEY_PREFIX = "user_queue_status:";
  private static final String TICKET_KEY_PREFIX = "match_ticket:";

  private static final int MAX_RATING_GAP = 200;

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void processMatching() {
    for (DistanceType distance : DistanceType.values()) {
      for (int targetCount = 2; targetCount <= 4; targetCount++) {
        tryMatch(distance, targetCount);
      }
    }
  }

  private void tryMatch(DistanceType distance, int targetCount) {
    String queueKey = QUEUE_KEY_PREFIX + distance.name() + ":" + targetCount;

    Set<TypedTuple<String>> matchedTuples =
        redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, targetCount - 1);

    if (matchedTuples == null || matchedTuples.size() < targetCount) {
      return;
    }

    List<TypedTuple<String>> tupleList = new ArrayList<>(matchedTuples);
    double minRating = Optional.ofNullable(tupleList.get(0).getScore()).orElse(0.0);
    double maxRating = Optional.ofNullable(tupleList.get(tupleList.size() - 1).getScore())
        .orElse(0.0);

    if ((maxRating - minRating) > MAX_RATING_GAP) {
      log.debug("매칭 보류 - 점수 격차 과다 (차이: {}점, 기준: {}점)", (maxRating - minRating), MAX_RATING_GAP);
      return;
    }

    Set<String> matchedUsers = matchedTuples.stream()
        .map(TypedTuple::getValue)
        .collect(Collectors.toSet());

    try {
      long currentTime = System.currentTimeMillis();
      long totalWaitTimeMs = 0;

      for (String userIdStr : matchedUsers) {
        String startTimeStr = redisTemplate.opsForValue().get("user_wait_start:" + userIdStr);

        if (startTimeStr != null) {
          long startTime = Long.parseLong(startTimeStr);
          totalWaitTimeMs += (currentTime - startTime); // (현재시간 - 시작시간) 합산
        }
      }

      int avgDuration = (int) ((totalWaitTimeMs / matchedUsers.size()) / 1000);
      // ==========================================================

      Long sessionId = matchSessionService.createOnlineSession(matchedUsers, distance, avgDuration);

      for (String userIdStr : matchedUsers) {
        Long userId = Long.parseLong(userIdStr);

        redisTemplate.opsForZSet().remove(queueKey, userIdStr);
        redisTemplate.delete(USER_STATUS_KEY_PREFIX + userId);

        redisTemplate.delete("user_wait_start:" + userId);

        redisTemplate.opsForValue()
            .set(TICKET_KEY_PREFIX + userId, sessionId.toString(), Duration.ofMinutes(1));
      }

      log.info("매칭 성사 - 세션ID: {}, 평균대기시간: {}초", sessionId, avgDuration);

    } catch (Exception e) {
      log.error("매칭 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

}

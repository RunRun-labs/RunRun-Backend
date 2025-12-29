package com.multi.runrunbackend.domain.match.scheduler;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
  private final RedissonClient redissonClient;


  private static final String QUEUE_KEY_PREFIX = "matching_queue:";
  private static final String USER_STATUS_KEY_PREFIX = "user_queue_status:";
  private static final String TICKET_KEY_PREFIX = "match_ticket:";
  private static final String WAIT_START_PREFIX = "user_wait_start:";

  private static final int MAX_RATING_GAP = 200;

  @Scheduled(fixedDelay = 1000)
  public void processMatching() {
    for (DistanceType distance : DistanceType.values()) {
      for (int targetCount = 2; targetCount <= 4; targetCount++) {
        tryMatch(distance, targetCount);
      }
    }
  }

  private void tryMatch(DistanceType distance, int targetCount) {
    String queueKey = QUEUE_KEY_PREFIX + distance.name() + ":" + targetCount;

    String lockKey = "lock:" + queueKey;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      if (lock.tryLock(0, 5, TimeUnit.SECONDS)) {

        Set<TypedTuple<String>> candidates = redisTemplate.opsForZSet()
            .rangeWithScores(queueKey, 0, targetCount - 1);

        if (candidates == null || candidates.size() < targetCount) {
          return;
        }

        List<TypedTuple<String>> list = new ArrayList<>(candidates);

        double minRating = Optional.ofNullable(list.get(0).getScore()).orElse(0.0);
        double maxRating = Optional.ofNullable(list.get(list.size() - 1).getScore()).orElse(0.0);

        if ((maxRating - minRating) > MAX_RATING_GAP) {
          return;
        }

        String[] userIds = list.stream().map(TypedTuple::getValue).toArray(String[]::new);

        Long removedCount = redisTemplate.opsForZSet().remove(queueKey, (Object[]) userIds);

        if (removedCount != null && removedCount == targetCount) {
          handleSuccess(list, distance, queueKey);

        } else {
          log.warn("매칭 실패: 타겟 {}명 중 {}명만 삭제됨. 복구 시도.", targetCount, removedCount);
          rollbackToQueue(list, queueKey);
        }
      }
    } catch (InterruptedException e) {
      log.error("락 획득 중 인터럽트 발생", e);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private void handleSuccess(List<TypedTuple<String>> userTuples, DistanceType distance,
      String queueKey) {
    Long sessionId = null;

    try {
      Set<String> matchedUsers = userTuples.stream()
          .map(TypedTuple::getValue)
          .collect(Collectors.toSet());

      long currentTime = System.currentTimeMillis();
      long totalWaitTimeMs = 0;

      for (String userIdStr : matchedUsers) {
        String startTimeStr = redisTemplate.opsForValue().get(WAIT_START_PREFIX + userIdStr);
        if (startTimeStr != null) {
          totalWaitTimeMs += (currentTime - Long.parseLong(startTimeStr));
        }
      }
      int avgDuration = (int) ((totalWaitTimeMs / matchedUsers.size()) / 1000);

      sessionId = matchSessionService.createOnlineSession(matchedUsers, distance, avgDuration);

    } catch (Exception e) {
      log.error("DB 세션 생성 실패. 유저를 대기열로 복구합니다.", e);
      rollbackToQueue(userTuples, queueKey);
      return;
    }

    try {
      for (TypedTuple<String> tuple : userTuples) {
        String userIdStr = tuple.getValue();
        Long userId = Long.parseLong(userIdStr);

        redisTemplate.opsForValue()
            .set(TICKET_KEY_PREFIX + userId, sessionId.toString(), Duration.ofMinutes(1));

        redisTemplate.delete(USER_STATUS_KEY_PREFIX + userId);
        redisTemplate.delete(WAIT_START_PREFIX + userId);
      }

      log.info("매칭 성사 완료 - 세션ID: {}", sessionId);

    } catch (Exception e) {
      log.error("매칭은 성공했으나 Redis 티켓 발행/정리 중 오류 발생. SessionID: {}", sessionId, e);
    }
  }

  private void rollbackToQueue(List<TypedTuple<String>> list, String queueKey) {
    Set<TypedTuple<String>> tuples = new HashSet<>(list);
    redisTemplate.opsForZSet().add(queueKey, tuples);
    log.info("대기열 일괄 복구 완료: {}명", tuples.size());
  }
}


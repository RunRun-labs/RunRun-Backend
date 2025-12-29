package com.multi.runrunbackend.domain.match.scheduler;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.match.service.MatchingQueueService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
  private final RedissonClient redissonClient;
  private final MatchSessionService matchSessionService;
  private final MatchingQueueService matchingQueueService;

  private static final int[] TARGET_COUNTS = {2, 3, 4};
  private static final int MAX_GAP = 200;
  private static final long LOCK_LEASE_SECONDS = 15;
  private static final long FIXED_DELAY_MS = 3000;

  private static final String QUEUE_KEY_PREFIX = "matching_queue:";
  private static final String USER_STATUS_KEY_PREFIX = "user_queue_status:";
  private static final String LOCK_PREFIX = "lock:";
  private static final String WAIT_START_PREFIX = "user_wait_start:";

  private static final int WINDOW_FACTOR = 6;
  private static final int MAX_STARTS = 5;

  private static final String MATCH_POP_SCRIPT = """
        local queueKey = KEYS[1]
        local targetCount = tonumber(ARGV[1])
        local maxGap = tonumber(ARGV[2])
        local statusPrefix = ARGV[3]
        local windowFactor = tonumber(ARGV[4])
        local maxStarts = tonumber(ARGV[5])
      
        local windowSize = targetCount * windowFactor
        if windowSize < targetCount then windowSize = targetCount end
      
        local rows = redis.call('ZRANGE', queueKey, 0, windowSize - 1, 'WITHSCORES')
        if (rows == nil or #rows == 0) then
          return nil
        end
      
        local candIds = {}
        local candScores = {}
      
        for i = 1, #rows, 2 do
          local userId = rows[i]
          local score = tonumber(rows[i + 1])
          local statusKey = statusPrefix .. userId
      
          if (redis.call('EXISTS', statusKey) == 0) then
            redis.call('ZREM', queueKey, userId) -- 좀비 즉시 제거
          else
            table.insert(candIds, userId)
            table.insert(candScores, score)
          end
        end
      
        if (#candIds < targetCount) then
          return nil
        end
      
        local maxStartIndex = #candIds - targetCount + 1
        local tries = maxStarts
        if tries > maxStartIndex then tries = maxStartIndex end
      
        for startIndex = 1, tries do
          local minScore = candScores[startIndex]
          local maxScore = candScores[startIndex]
      
          for j = startIndex + 1, startIndex + targetCount - 1 do
            local s = candScores[j]
            if s < minScore then minScore = s end
            if s > maxScore then maxScore = s end
          end
      
          if ((maxScore - minScore) <= maxGap) then
            -- 통과: pop
            local out = {}
            for j = startIndex, startIndex + targetCount - 1 do
              redis.call('ZREM', queueKey, candIds[j])
              out[#out + 1] = candIds[j] .. '|' .. tostring(candScores[j])
            end
            return out
          end
        end
      
        return nil
      """;

  @Scheduled(fixedDelay = FIXED_DELAY_MS)
  public void runMatching() {
    for (DistanceType distance : DistanceType.values()) {
      for (int targetCount : TARGET_COUNTS) {
        String queueKey = makeQueueKey(distance, targetCount);
        tryMatch(queueKey, distance, targetCount);
      }
    }
  }

  private void tryMatch(String queueKey, DistanceType distance, int targetCount) {
    String lockKey = LOCK_PREFIX + queueKey;
    RLock lock = redissonClient.getLock(lockKey);

    boolean locked = false;
    try {
      locked = lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
      if (!locked) {
        return;
      }

      List<UserWithScore> picked = popCandidatesAtomically(queueKey, targetCount, MAX_GAP);
      if (picked.isEmpty()) {
        return;
      }

      List<Long> userIds = picked.stream().map(UserWithScore::userId).toList();

      Set<String> userIdSet = userIds.stream().map(String::valueOf).collect(Collectors.toSet());

      int avgDuration = computeAvgWaitMinutes(userIds);

      try {
        Long sessionId = matchSessionService.createOnlineSession(
            userIdSet,
            distance,
            avgDuration
        );
        matchingQueueService.issueMatchTickets(sessionId, userIds);

        matchingQueueService.cleanupAfterMatched(userIds);

        log.info("매칭 성공 - Queue: {}, distance: {}, targetCount: {}, sessionId: {}, users: {}",
            queueKey, distance, targetCount, sessionId, userIds);

      } catch (Exception e) {
        rollbackToQueueSafe(queueKey, picked);
        log.error("매칭 처리 실패(복구 수행) - Queue: {}, users: {}", queueKey, userIds, e);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("매칭 락 획득 중 인터럽트 - Queue: {}", queueKey, e);
    } finally {
      if (locked && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<UserWithScore> popCandidatesAtomically(String queueKey, int targetCount,
      int maxGap) {
    List<String> result = (List<String>) redisTemplate.execute(
        new DefaultRedisScript<>(MATCH_POP_SCRIPT, List.class),
        List.of(queueKey),
        String.valueOf(targetCount),
        String.valueOf(maxGap),
        USER_STATUS_KEY_PREFIX,
        String.valueOf(WINDOW_FACTOR),
        String.valueOf(MAX_STARTS)
    );

    if (result == null || result.isEmpty()) {
      return List.of();
    }

    return result.stream()
        .filter(Objects::nonNull)
        .filter(s -> s.contains("|"))
        .map(s -> {
          String[] parts = s.split("\\|");
          Long userId = Long.parseLong(parts[0]);
          int score = (int) Double.parseDouble(parts[1]);
          return new UserWithScore(userId, score);
        })
        .toList();
  }

  private void rollbackToQueueSafe(String queueKey, List<UserWithScore> picked) {
    for (UserWithScore u : picked) {
      String statusKey = USER_STATUS_KEY_PREFIX + u.userId();
      Boolean exists = redisTemplate.hasKey(statusKey);
      if (Boolean.TRUE.equals(exists)) {
        redisTemplate.opsForZSet().add(queueKey, u.userId().toString(), u.score());
      }
    }
  }

  private String makeQueueKey(DistanceType distance, int targetCount) {
    return QUEUE_KEY_PREFIX + distance.name() + ":" + targetCount;
  }

  private record UserWithScore(Long userId, int score) {

  }

  private int computeAvgWaitMinutes(List<Long> userIds) {
    long now = System.currentTimeMillis();

    long sumMinutes = 0L;
    int counted = 0;

    for (Long userId : userIds) {
      String ts = redisTemplate.opsForValue().get(WAIT_START_PREFIX + userId);
      if (ts == null) {
        continue;
      }

      try {
        long start = Long.parseLong(ts);
        long waitedMs = Math.max(0L, now - start);
        sumMinutes += TimeUnit.MILLISECONDS.toMinutes(waitedMs);
        counted++;
      } catch (NumberFormatException ignore) {
      }
    }

    if (counted == 0) {
      return 0;
    }

    return (int) (sumMinutes / counted);
  }
}

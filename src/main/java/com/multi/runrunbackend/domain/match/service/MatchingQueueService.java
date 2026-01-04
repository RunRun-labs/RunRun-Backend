package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.OnlineMatchStatusResDto;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.rating.entity.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : MatchingQueueService
 * @since : 2025-12-27 토요일
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingQueueService {

  private final RedisTemplate<String, String> redisTemplate;
  private final DistanceRatingRepository distanceRatingRepository;
  private final UserRepository userRepository;
  private final SessionUserRepository sessionUserRepository;
  private final RedissonClient redissonClient;

  private static final String QUEUE_KEY_PREFIX = "matching_queue:";
  private static final String USER_STATUS_KEY_PREFIX = "user_queue_status:";
  private static final String TICKET_KEY_PREFIX = "match_ticket:";
  private static final String WAIT_START_PREFIX = "user_wait_start:";

  private static final String LOCK_PREFIX = "lock:";

  private static final String ADD_QUEUE_ATOMIC_SCRIPT = """
      local newQueueKey = KEYS[1]
      local statusKey = KEYS[2]
      local waitStartKey = KEYS[3]
      
      local userId = ARGV[1]
      local score = ARGV[2]
      local ttlSeconds = ARGV[3]
      local newQueueKeyVal = ARGV[4]
      local timeNow = ARGV[5]
      
      local oldQueueKey = redis.call('GET', statusKey)
      
      if oldQueueKey and oldQueueKey ~= newQueueKey then
          redis.call('ZREM', oldQueueKey, userId)
      end
      
      redis.call('ZADD', newQueueKey, score, userId)
      
      redis.call('SET', statusKey, newQueueKeyVal, 'EX', ttlSeconds)
      redis.call('SET', waitStartKey, timeNow, 'EX', ttlSeconds)
      
      return 1
      """;

  private static final String REMOVE_QUEUE_SCRIPT = """
      local statusKey = KEYS[1]
      local waitStartKey = KEYS[2]
      local userId = ARGV[1]
      
      local queueKey = redis.call('GET', statusKey)
      
      if (queueKey) then
          redis.call('ZREM', queueKey, userId)
          redis.call('DEL', statusKey)
          redis.call('DEL', waitStartKey)
          return queueKey
      else
          return nil
      end
      """;

  @Transactional
  public Long addQueue(CustomUser principal, DistanceType distance, int targetCount) {

    String loginId = principal.getLoginId();
    User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
        ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();

    Optional<SessionUser> activeSession = sessionUserRepository.findActiveOnlineSession(userId);
    if (activeSession.isPresent()) {
      Long existingSessionId = activeSession.get().getMatchSession().getId();
      log.info("이미 매칭된 세션 존재 - User: {}, SessionID: {}", userId, existingSessionId);
      return existingSessionId;
    }

    int rating = distanceRatingRepository.findByUserIdAndDistanceType(userId, distance)
        .map(DistanceRating::getCurrentRating)
        .orElse(1000);

    String queueKey = makeQueueKey(distance, targetCount);
    String statusKey = USER_STATUS_KEY_PREFIX + userId;
    String waitStartKey = WAIT_START_PREFIX + userId;

    redisTemplate.execute(
        new DefaultRedisScript<>(ADD_QUEUE_ATOMIC_SCRIPT, Long.class), // 반환 타입 Long 명시
        List.of(queueKey, statusKey, waitStartKey),
        userId.toString(),
        String.valueOf(rating),
        "180", // TTL 3분
        queueKey,
        String.valueOf(System.currentTimeMillis())
    );

    log.info("매칭 대기열 등록 완료 (Atomic+Cleanup) - User: {}, Queue: {}", userId, queueKey);
    return null;
  }

  @Transactional
  public void removeQueue(CustomUser principal) {

    User user = userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();

    String statusKey = USER_STATUS_KEY_PREFIX + userId;
    String waitStartKey = WAIT_START_PREFIX + userId;
    String queueKey = redisTemplate.opsForValue().get(statusKey);
    if (queueKey == null) {
      log.debug("매칭 취소 시도했으나 대기 중 아님 - User: {}", userId);
      return;
    }

    String lockKey = LOCK_PREFIX + queueKey;
    RLock lock = redissonClient.getLock(lockKey);

    boolean locked = false;
    try {
      locked = lock.tryLock(200, 3, TimeUnit.SECONDS);
      if (!locked) {
        log.debug("취소 락 획득 실패(그래도 취소 진행) - User: {}, Queue: {}", userId, queueKey);
      }

      String removedQueueKey = redisTemplate.execute(
          new DefaultRedisScript<>(REMOVE_QUEUE_SCRIPT, String.class),
          List.of(statusKey, waitStartKey),
          userId.toString()
      );

      if (removedQueueKey != null) {
        log.info("매칭 대기열 취소 (락 공유 + Atomic) - User: {}, Queue: {}", userId, removedQueueKey);
      } else {
        log.debug("취소 처리 중 이미 대기열에서 제거됨 - User: {}", userId);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("취소 락 획득 중 인터럽트 발생 - User: {}, Queue: {}", userId, queueKey, e);
    } finally {
      if (locked && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Transactional
  public OnlineMatchStatusResDto checkMatchStatus(CustomUser principal) {

    String loginId = principal.getLoginId();
    User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
        ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();

    String ticketKey = TICKET_KEY_PREFIX + userId;
    String sessionId = redisTemplate.opsForValue().get(ticketKey);

    if (sessionId != null) {

      return OnlineMatchStatusResDto.builder()
          .status("MATCHED")
          .sessionId(Long.parseLong(sessionId))
          .build();
    }

    String statusKey = USER_STATUS_KEY_PREFIX + userId;
    String queueKey = redisTemplate.opsForValue().get(USER_STATUS_KEY_PREFIX + userId);

    if (queueKey != null) {

      redisTemplate.expire(statusKey, Duration.ofMinutes(3));
      redisTemplate.expire(WAIT_START_PREFIX + userId, Duration.ofMinutes(3));

      return OnlineMatchStatusResDto.builder()
          .status("WAITING")
          .sessionId(null)
          .build();
    }

    Optional<SessionUser> activeSession = sessionUserRepository.findActiveOnlineSession(userId);

    if (activeSession.isPresent()) {
      Long dbSessionId = activeSession.get().getMatchSession().getId();

      redisTemplate.opsForValue().set(ticketKey, dbSessionId.toString(), Duration.ofMinutes(1));

      log.info("Redis 티켓 소실 감지 -> DB 폴백으로 매칭 확인 - User: {}, SessionID: {}", userId, dbSessionId);

      return OnlineMatchStatusResDto.builder()
          .status("MATCHED")
          .sessionId(dbSessionId)
          .build();
    }

    return OnlineMatchStatusResDto.builder()
        .status("NONE")
        .sessionId(null)
        .build();
  }

  @Transactional
  public void cleanupAfterMatched(List<Long> userIds) {
    for (Long userId : userIds) {
      String statusKey = USER_STATUS_KEY_PREFIX + userId;
      String waitStartKey = WAIT_START_PREFIX + userId;

      redisTemplate.delete(statusKey);
      redisTemplate.delete(waitStartKey);
    }
  }

  @Transactional
  public void issueMatchTickets(Long sessionId, List<Long> userIds) {
    for (Long userId : userIds) {
      String ticketKey = TICKET_KEY_PREFIX + userId; // "match_ticket:"
      redisTemplate.opsForValue().set(
          ticketKey,
          sessionId.toString(),
          Duration.ofMinutes(1)
      );
    }
  }

  private String makeQueueKey(DistanceType distance, int targetCount) {
    return QUEUE_KEY_PREFIX + distance.name() + ":" + targetCount;
  }

}

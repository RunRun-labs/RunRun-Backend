package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.OnlineMatchStatusResDto;
import com.multi.runrunbackend.domain.rating.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
  private static final String QUEUE_KEY_PREFIX = "matching_queue:";
  private static final String USER_STATUS_KEY_PREFIX = "user_queue_status:";
  private static final String TICKET_KEY_PREFIX = "match_ticket:";
  private static final String WAIT_START_PREFIX = "user_wait_start:";

  @Transactional
  public void addQueue(CustomUser principal, DistanceType distance, int targetCount) {

    String loginId = principal.getLoginId();
    User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
        ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();
    removeQueue(principal);

    int rating = distanceRatingRepository.findByUserIdAndDistanceType(userId, distance)
        .map(DistanceRating::getCurrentRating)
        .orElse(1000);

    String queueKey = makeQueueKey(distance, targetCount);

    redisTemplate.opsForZSet().add(queueKey, userId.toString(), rating);

    redisTemplate.opsForValue()
        .set(WAIT_START_PREFIX + userId, String.valueOf(System.currentTimeMillis()));

    redisTemplate.opsForValue().set(USER_STATUS_KEY_PREFIX + userId, queueKey);

    log.info("매칭 대기열 등록 완료 - User: {}, Queue: {}, Rating: {}", userId, queueKey, rating);
  }

  public void removeQueue(CustomUser principal) {

    String loginId = principal.getLoginId();
    User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
        ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();
    String statusKey = USER_STATUS_KEY_PREFIX + userId;
    String queueKey = redisTemplate.opsForValue().get(statusKey);

    if (queueKey != null) {
      redisTemplate.opsForZSet().remove(queueKey, userId.toString());
      redisTemplate.delete(statusKey);
      log.info("매칭 대기열 취소 - User: {}, Queue: {}", userId, queueKey);

      redisTemplate.delete(WAIT_START_PREFIX + userId);
    }
  }

  public OnlineMatchStatusResDto checkMatchStatus(CustomUser principal) {

    String loginId = principal.getLoginId();
    User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
        ErrorCode.USER_NOT_FOUND));
    Long userId = user.getId();

    String ticketKey = TICKET_KEY_PREFIX + userId;
    String sessionId = redisTemplate.opsForValue().get(ticketKey);

    if (sessionId != null) {

      redisTemplate.delete(ticketKey);

      log.info("매칭 티켓 소모 완료 - User: {}, SessionID: {}", userId, sessionId);

      return OnlineMatchStatusResDto.builder()
          .status("MATCHED")
          .sessionId(Long.parseLong(sessionId))
          .build();
    }

    String queueKey = redisTemplate.opsForValue().get(USER_STATUS_KEY_PREFIX + userId);
    if (queueKey != null) {
      return OnlineMatchStatusResDto.builder()
          .status("WAITING")
          .sessionId(null)
          .build();
    }

    return OnlineMatchStatusResDto.builder()
        .status("NONE")
        .sessionId(null)
        .build();
  }

  private String makeQueueKey(DistanceType distance, int targetCount) {
    return QUEUE_KEY_PREFIX + distance.name() + ":" + targetCount;
  }

}

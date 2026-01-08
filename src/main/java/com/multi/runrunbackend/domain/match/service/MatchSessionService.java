package com.multi.runrunbackend.domain.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.chat.repository.OfflineChatMessageRepository;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningResultFilterType;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.constant.Tier;
import com.multi.runrunbackend.domain.match.dto.req.SoloRunStartReqDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingInfoDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingParticipantDto;
import com.multi.runrunbackend.domain.match.dto.res.RunningRecordResDto;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.rating.entity.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.recruit.repository.RecruitUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : MatchService
 * @since : 2025-12-21 일요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchSessionService {

  private final RecruitRepository recruitRepository;
  private final UserRepository userRepository;
  private final MatchSessionRepository matchSessionRepository;
  private final RecruitUserRepository recruitUserRepository;
  private final SessionUserRepository sessionUserRepository;
  private final RunningResultRepository runningResultRepository;
  private final CourseRepository courseRepository;
  private final OfflineChatMessageRepository chatMessageRepository;  // ⭐ 추가
  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, Object> redisPubSubTemplate;  // ✅ Redis Pub/Sub 추가
  private final RedisTemplate<String, String> redisTemplate;  // ✅ Redis Ticket 삭제용
  private final ObjectMapper objectMapper;  // ✅ JSON 변환용
  private final DistanceRatingRepository distanceRatingRepository;
  private final MatchingQueueService matchingQueueService;  // ✅ 매칭 큐 서비스


  @Transactional
  public Long createOfflineSession(Long recruitId, CustomUser principal) {

    User user = getUser(principal);
    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

    if (!recruit.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.UNAUTHORIZED_HOST);
    }

    if (recruit.getCurrentParticipants() < 2) {
      throw new ValidationException(ErrorCode.NOT_ENOUGH_PARTICIPANTS);
    }

    LocalDateTime allowedStartTime = recruit.getMeetingAt().minusHours(3);
    if (LocalDateTime.now().isBefore(allowedStartTime)) {
      throw new ValidationException(ErrorCode.TOO_EARLY_TO_START);
    }

    return createSessionInternal(recruit);
  }

  @Transactional
  public void createOfflineSessionBySystem(Long recruitId) {
    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

    if (recruit.getCurrentParticipants() < 2) {
      recruitRepository.delete(recruit);
      return;
    }

    createSessionInternal(recruit);
  }

  private Long createSessionInternal(Recruit recruit) {
    if (matchSessionRepository.existsByRecruit(recruit)) {
      return matchSessionRepository.findByRecruit(recruit).get().getId();
    }
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime recruitCreatedAt = recruit.getCreatedAt();

    long waitingTime = ChronoUnit.MINUTES.between(recruitCreatedAt, now);

    MatchSession matchSession = MatchSession.builder()
        .recruit(recruit)
        .course(recruit.getCourse())
        .type(SessionType.OFFLINE)
        .status(SessionStatus.STANDBY)
        .duration((int) waitingTime)
        .targetDistance(recruit.getTargetDistance())
        .build();

    matchSessionRepository.save(matchSession);

    // ⭐ 세션 ID가 재사용되었을 경우 MongoDB의 과거 메시지 삭제
    int deletedCount = chatMessageRepository.deleteBySessionId(matchSession.getId());
    if (deletedCount > 0) {
      log.info("⭐ 오프라인 세션 생성: sessionId={}, 과거 메시지 {} 개 삭제",
          matchSession.getId(), deletedCount);
    }

    List<RecruitUser> participants = recruitUserRepository.findAllByRecruitId(
        recruit.getId());

    List<SessionUser> sessionUsers = participants.stream()
        .map(p -> SessionUser.builder()
            .matchSession(matchSession)
            .user(p.getUser())
            .isReady(false)
            .build())
        .collect(Collectors.toList());

    User host = recruit.getUser();

    boolean isHostAlreadyIncluded = sessionUsers.stream()
        .anyMatch(u -> u.getUser().getId().equals(host.getId()));

    if (!isHostAlreadyIncluded) {
      SessionUser hostSessionUser = SessionUser.builder()
          .matchSession(matchSession)
          .user(host)
          .isReady(false)
          .build();

      sessionUsers.add(hostSessionUser);
    }
    sessionUserRepository.saveAll(sessionUsers);

    recruit.updateStatus(RecruitStatus.MATCHED);

    return matchSession.getId();
  }

  @Transactional
  public Long createOnlineSession(Set<String> userIds, DistanceType distance, int avgDuration) {

    if (userIds == null || userIds.isEmpty()) {
      throw new ValidationException(ErrorCode.NOT_ENOUGH_PARTICIPANTS);
    }

    double targetDistanceValue = convertToKilometer(distance);

    MatchSession session = MatchSession.builder()
        .type(SessionType.ONLINE)
        .targetDistance(targetDistanceValue)
        .duration(avgDuration)
        .status(SessionStatus.STANDBY)
        .build();

    matchSessionRepository.save(session);

    // ⭐ 세션 ID가 재사용되었을 경우 MongoDB의 과거 메시지 삭제
    int deletedCount = chatMessageRepository.deleteBySessionId(session.getId());
    if (deletedCount > 0) {
      log.info("⭐ 온라인 세션 생성: sessionId={}, 과거 메시지 {} 개 삭제",
          session.getId(), deletedCount);
    }

    List<SessionUser> sessionUsers = new ArrayList<>();

    for (String userIdStr : userIds) {
      Long userId = Long.parseLong(userIdStr);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

      SessionUser sessionUser = SessionUser.builder()
          .matchSession(session)
          .user(user)
          .isReady(false)
          .build();

      sessionUsers.add(sessionUser);
    }

    sessionUserRepository.saveAll(sessionUsers);

    log.info("온라인 매칭 DB 저장 완료 - SessionID: {}, 거리: {}km", session.getId(), targetDistanceValue);

    return session.getId();
  }


  /**
   * 대기방 정보 조회
   */
  public MatchWaitingInfoDto getWaitingInfo(Long sessionId, Long currentUserId) {
    log.info("🔍 세션 정보 조회 시작: sessionId={}, currentUserId={}", sessionId, currentUserId);

    // 세션 조회
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    log.info("✅ 세션 찾음: id={}, status={}, targetDistance={}",
        session.getId(), session.getStatus(), session.getTargetDistance());

    // 참가자 목록 조회
    List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

    log.info("👥 참가자 수: {}", sessionUsers.size());

    if (sessionUsers.isEmpty()) {
      log.error("❌ 참가자가 없음! sessionId={}", sessionId);
      throw new NotFoundException(ErrorCode.SESSION_NOT_FOUND);
    }

    // 방장 찾기 (첫 번째 참가자 또는 Recruit의 host)
    Long hostUserId = session.getRecruit() != null
        ? session.getRecruit().getUser().getId()
        : sessionUsers.get(0).getUser().getId();

    log.info("👑 방장 userId: {}", hostUserId);

    // targetDistance를 기반으로 DistanceType 결정
    DistanceType distanceType = determineDistanceType(session.getTargetDistance());

    // 참가자 DTO 변환
    List<MatchWaitingParticipantDto> participants = sessionUsers.stream()
        .map(su -> {
          User user = su.getUser();

          // 티어 정보 조회
          Tier tier =
              distanceRatingRepository.findByUserIdAndDistanceType(user.getId(),
                      distanceType)
                  .map(DistanceRating::getCurrentTier)
                  .orElse(Tier.거북이);

          return MatchWaitingParticipantDto.builder()
              .userId(user.getId())
              .name(user.getName())
              .profileImage(user.getProfileImageUrl())
              .isReady(su.isReady())
              .isHost(user.getId().equals(hostUserId))
              .avgPace("5:" + (30 + (int) (Math.random() * 30)))  // 임시 하드코딩: 5:30 ~ 5:59
              .tier(tier)
              .build();
        })
        .collect(Collectors.toList());

    // Ready 카운트
    long readyCount = sessionUsers.stream().filter(SessionUser::isReady).count();

    // 남은 시간 계산 (세션 생성 시각 + 5분 - 현재 시각)
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime createdAt = session.getCreatedAt();
    LocalDateTime timeLimit = createdAt.plusMinutes(5);  // 5분 제한

    long remainingSeconds = Duration.between(now, timeLimit).getSeconds();
    if (remainingSeconds < 0) {
      remainingSeconds = 0;  // 음수면 0으로
    }

    MatchWaitingInfoDto result = MatchWaitingInfoDto.builder()
        .sessionId(session.getId())
        .targetDistance(session.getTargetDistance())
        .status(session.getStatus())
        .createdAt(session.getCreatedAt())
        .remainingSeconds(remainingSeconds)
        .participants(participants)
        .readyCount((int) readyCount)
        .totalCount(sessionUsers.size())
        .build();

    log.info("✅ 세션 정보 반환: participants={}, readyCount={}",
        result.getTotalCount(), result.getReadyCount());

    return result;
  }


  @Transactional
  public Long createGhostSession(Long runningResultId, CustomUser principal) {

    User user = getUser(principal);

    RunningResult ghostResult = runningResultRepository.findById(runningResultId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND));

    if (!ghostResult.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.UNAUTHORIZED);
    }

    MatchSession session = MatchSession.builder()
        .type(SessionType.GHOST)
        .runningResult(ghostResult)
        .targetDistance(ghostResult.getTotalDistance().doubleValue())
        .status(SessionStatus.STANDBY)
        .duration(0)
        .build();

    matchSessionRepository.save(session);

    // ⭐ 세션 ID가 재사용되었을 경우 MongoDB의 과거 메시지 삭제
    int deletedCount = chatMessageRepository.deleteBySessionId(session.getId());
    if (deletedCount > 0) {
      log.info("⭐ 고스트 세션 생성: sessionId={}, 과거 메시지 {} 개 삭제",
          session.getId(), deletedCount);
    }

    SessionUser sessionUser = SessionUser.builder()
        .matchSession(session)
        .user(user)
        .isReady(false)
        .build();

    sessionUserRepository.save(sessionUser);

    log.info("고스트 세션 생성 - SessionID: {}, GhostResultID: {}", session.getId(), runningResultId);

    return session.getId();
  }

  public Slice<RunningRecordResDto> getMyRunningRecords(CustomUser principal,
      RunningResultFilterType filterType, Pageable pageable) {
    User user = getUser(principal);

    BigDecimal min = filterType != null ? switch (filterType) {
      case UNDER_3 -> BigDecimal.ZERO;
      case BETWEEN_3_5 -> BigDecimal.valueOf(3.0);
      case BETWEEN_5_10 -> BigDecimal.valueOf(5.0);
      case OVER_10 -> BigDecimal.valueOf(10.0);
      case ALL -> null;
    } : null;

    BigDecimal max = filterType != null ? switch (filterType) {
      case UNDER_3 -> BigDecimal.valueOf(3.0);
      case BETWEEN_3_5 -> BigDecimal.valueOf(5.0);
      case BETWEEN_5_10 -> BigDecimal.valueOf(10.0);
      case OVER_10 -> null;
      case ALL -> null;
    } : null;

    Slice<RunningResult> resultSlice = runningResultRepository.findMySoloRecordsByDistance(
        user.getId(),
        RunStatus.COMPLETED,
        min,
        max,
        pageable
    );

    return resultSlice.map(RunningRecordResDto::from);
  }

  @Transactional
  public Long createSoloSession(CustomUser principal, SoloRunStartReqDto reqDto) {

    User user = getUser(principal);

    Long courseId = reqDto.getCourseId();

    if (courseId == null && reqDto.getDistance() == null) {
      throw new BadRequestException(ErrorCode.DISTANCE_REQUIRED);
    }

    Course course = null;
    Double distance = null;

    if (courseId != null) {
      course = courseRepository.findById(courseId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));
      distance = course.getDistanceM() / 1000.0;
    } else if (reqDto.getManualDistance() != null) {
      distance = reqDto.getManualDistance();
    } else if (reqDto.getDistance() != null) {
      distance = convertToKilometer(reqDto.getDistance());
    } else {
      throw new BadRequestException(ErrorCode.DISTANCE_REQUIRED);
    }

    MatchSession session = MatchSession.builder()
        .duration(0)
        .status(SessionStatus.STANDBY)
        .targetDistance(distance)
        .type(SessionType.SOLO)
        .course(course)
        .build();

    matchSessionRepository.save(session);

    // ⭐ 세션 ID가 재사용되었을 경우 MongoDB의 과거 메시지 삭제
    int deletedCount = chatMessageRepository.deleteBySessionId(session.getId());
    if (deletedCount > 0) {
      log.info("⭐ 솔로 세션 생성: sessionId={}, 과거 메시지 {} 개 삭제",
          session.getId(), deletedCount);
    }

    SessionUser sessionUser = SessionUser.builder()
        .matchSession(session)
        .user(user)
        .isReady(false)
        .build();

    sessionUserRepository.save(sessionUser);

    log.info("솔로 세션 생성 완료 - SessionID: {}", session.getId());
    return session.getId();
  }

  /**
   * 고스트런 세션 정보 조회
   */
  public Map<String, Object> getGhostSessionInfo(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    RunningResult ghostRecord = session.getRunningResult();
    if (ghostRecord == null) {
      throw new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND);
    }

    Map<String, Object> info = new HashMap<>();
    info.put("sessionId", session.getId());
    info.put("targetDistance", session.getTargetDistance());
    info.put("ghostRecord", Map.of(
        "id", ghostRecord.getId(),
        "totalDistance", ghostRecord.getTotalDistance(),
        "totalTime", ghostRecord.getTotalTime(),
        "avgPace", ghostRecord.getAvgPace(),
        "startedAt", ghostRecord.getStartedAt(),
        "splitPace", ghostRecord.getSplitPace()
    ));

    log.info("✅ 고스트 세션 정보 조회: sessionId={}, ghostRecordId={}",
        sessionId, ghostRecord.getId());

    return info;
  }


  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  private double convertToKilometer(DistanceType distance) {
    return switch (distance) {
      case KM_3 -> 3.0;
      case KM_5 -> 5.0;
      case KM_10 -> 10.0;
      default -> throw new ValidationException(ErrorCode.INVALID_DISTANCE_TYPE);
    };
  }

  /**
   * 대기방에서 나가기 (세션 취소)
   */
  @Transactional
  public void leaveSession(Long sessionId, Long userId) {
    try {
      log.info("🚶 대기방 나가기 시작: sessionId={}, userId={}", sessionId, userId);

      // 세션 조회
      MatchSession session = matchSessionRepository.findById(sessionId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

      log.info("✅ 세션 조회 성공: sessionId={}, status={}", sessionId, session.getStatus());

      // 이미 시작한 경우
      if (session.getStatus() == SessionStatus.IN_PROGRESS) {
        throw new ValidationException(ErrorCode.ALREADY_IN_PROGRESS);
      }

      // 사용자 조회
      SessionUser sessionUser = sessionUserRepository
          .findBySessionIdAndUserId(sessionId, userId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

      User leavingUser = sessionUser.getUser();

      log.info("✅ 사용자 조회 성공: userId={}, username={}", userId, leavingUser.getName());

      // ✅ 1. SessionUser soft delete
      sessionUser.delete();
      sessionUserRepository.save(sessionUser);

      log.info("✅ SessionUser soft delete 성공");

      // 남은 참가자 확인
      List<SessionUser> remainingUsers = sessionUserRepository.findActiveUsersBySessionId(
          sessionId);
      int remainingCount = remainingUsers.size();

      log.info("👥 남은 참가자: {}명", remainingCount);

      // ✅ 온라인 배틀은 최소 2명 필요 - 2명 미만이면 세션 취소
      if (remainingCount < 2) {
        log.info("❌ 참가자 부족({}/2명) - 세션 취소 시작", remainingCount);

        // ✅ 2. 세션 상태 변경 (CANCELLED로 변경)
        try {
          session.updateStatus(SessionStatus.CANCELLED);
          matchSessionRepository.save(session);
          log.info("✅ 세션 상태 변경 성공: CANCELLED");
        } catch (Exception e) {
          log.error("❌ 세션 상태 변경 실패", e);
          throw e;
        }

        // ✅ 3. 남은 참가자들도 큐에서 제거 및 매칭 세션 Redis 키 삭제 (세션 취소 시)
        if (remainingCount > 0) {
          for (SessionUser remainingUser : remainingUsers) {
            try {
              Long remainingUserId = remainingUser.getUser().getId();
              matchingQueueService.removeQueueByUserId(remainingUserId);
              matchingQueueService.cleanupMatchSession(remainingUserId);
              log.info("✅ 남은 참가자 큐 및 세션 키 제거 - User: {}", remainingUserId);
            } catch (Exception e) {
              log.error("❌ 남은 참가자 정리 실패 - User: {}",
                  remainingUser.getUser().getId(), e);
              // 큐 제거 실패해도 계속 진행
            }
          }
        }
        
        // ✅ 나간 사용자의 Redis 키도 삭제
        try {
          matchingQueueService.cleanupMatchSession(userId);
          log.info("✅ 나간 사용자 세션 키 제거 - User: {}", userId);
        } catch (Exception e) {
          log.error("❌ 나간 사용자 세션 키 제거 실패 - User: {}", userId, e);
        }

        // ✅ 4. 남은 참가자들에게 취소 알림 (WebSocket)
        if (remainingCount > 0) {
          try {
            sendSessionCancelMessage(sessionId, leavingUser.getName());
            log.info("✅ 취소 메시지 전송 성공: {}명에게 알림", remainingCount);
          } catch (Exception e) {
            log.error("❌ 취소 메시지 전송 실패", e);
            // 메시지 실패는 무시하고 계속
          }
        }
      } else {
        // 2명 이상 남음 - 계속 진행
        log.info("✅ 충분한 참가자 남음({}/2명) - 계속 진행", remainingCount);

        // ✅ 참가자 나간 것만 알림
        try {
          sendUserLeftMessage(sessionId, leavingUser.getName(), remainingCount);
        } catch (Exception e) {
          log.error("❌ 사용자 이탈 메시지 전송 실패", e);
        }
      }

      log.info("✅ leaveSession 완료: sessionId={}, userId={}", sessionId, userId);

    } catch (Exception e) {
      log.error("❌❌❌ leaveSession 실패: sessionId={}, userId={}", sessionId, userId, e);
      log.error("❌ 에러 메시지: {}", e.getMessage());
      log.error("❌ 에러 타입: {}", e.getClass().getName());
      throw e;  // 예외 다시 던지기
    }
  }

  /**
   * 세션 취소 메시지 전송 (WebSocket) - ✅ Redis Pub/Sub 사용
   */
  private void sendSessionCancelMessage(Long sessionId, String leaverName) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "SESSION_CANCELLED");
    message.put("message", leaverName + "님이 나가서 매칭이 취소되었습니다.");
    message.put("leaverName", leaverName);
    message.put("timestamp", LocalDateTime.now());

    String destination = "/sub/battle/" + sessionId + "/cancel";
    publishToRedis(destination, message);  // ✅ Redis로 발행

    log.info("📤 세션 취소 메시지 전송: sessionId={}, leaver={}", sessionId, leaverName);
  }

  /**
   * targetDistance를 기반으로 DistanceType 결정
   */
  private DistanceType determineDistanceType(Double targetDistance) {
    if (targetDistance != null && Math.abs(targetDistance - 3.0) < 0.01) {
      return DistanceType.KM_3;
    } else if (targetDistance != null && Math.abs(targetDistance - 5.0) < 0.01) {
      return DistanceType.KM_5;
    } else {
      return DistanceType.KM_10;
    }
  }

  /**
   * 참가자 나간 알림 메시지 전송 (WebSocket) - ✅ Redis Pub/Sub 사용
   */
  private void sendUserLeftMessage(Long sessionId, String leaverName, int remainingCount) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "USER_LEFT");
    message.put("message", leaverName + "님이 나갔습니다. (남은 인원: " + remainingCount + "명)");
    message.put("leaverName", leaverName);
    message.put("remainingCount", remainingCount);
    message.put("timestamp", LocalDateTime.now());

    String destination = "/sub/battle/" + sessionId + "/user-left";
    publishToRedis(destination, message);  // ✅ Redis로 발행

    log.info("📤 참가자 이탈 메시지 전송: sessionId={}, leaver={}, remaining={}",
        sessionId, leaverName, remainingCount);
  }

  /**
   * ✅ Redis Pub/Sub을 통한 메시지 발행 (다중 서버 환경 지원)
   */
  private void publishToRedis(String destination, Object message) {
    try {
      Map<String, Object> redisMessage = new HashMap<>();
      redisMessage.put("destination", destination);
      redisMessage.put("message", message);

      // ✅ 간단한 채널명 사용 (hashCode 대신 sessionId 직접 사용)
      String channel = "battle:session";
      String payload = objectMapper.writeValueAsString(redisMessage);

      log.info("📤 [Redis Pub] 발행 시도 - channel: {}, destination: {}", channel, destination);

      redisPubSubTemplate.convertAndSend(channel, payload);

      log.info("✅ [Redis Pub] 발행 성공 - channel: {}, payload length: {}", channel, payload.length());

    } catch (Exception e) {
      log.error("❌ Redis Pub 실패: destination={}", destination, e);
      log.error("❌ 에러 상세: {}", e.getMessage());
    }
  }
}


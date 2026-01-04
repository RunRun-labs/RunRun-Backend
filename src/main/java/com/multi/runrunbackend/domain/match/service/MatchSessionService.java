package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningResultFilterType;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingInfoDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingParticipantDto;
import com.multi.runrunbackend.domain.match.dto.res.RunningRecordResDto;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
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

  private double convertToKilometer(DistanceType distance) {
    return switch (distance) {
      case KM_3 -> 3.0;
      case KM_5 -> 5.0;
      case KM_10 -> 10.0;
      default -> 0.0;
    };
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

    // 참가자 DTO 변환
    List<MatchWaitingParticipantDto> participants = sessionUsers.stream()
        .map(su -> {
          User user = su.getUser();
          return MatchWaitingParticipantDto.builder()
              .userId(user.getId())
              .name(user.getName())
              .profileImage(user.getProfileImageUrl())
              .isReady(su.isReady())
              .isHost(user.getId().equals(hostUserId))
              .avgPace("5:" + (30 + (int) (Math.random() * 30)))  // 임시 하드코딩: 5:30 ~ 5:59
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
}


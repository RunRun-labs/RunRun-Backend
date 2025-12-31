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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

    double targetDistanceValue = convertToKiloMeter(distance);

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

  private double convertToKiloMeter(DistanceType distance) {
    return switch (distance) {
      case KM_3 -> 3.0;
      case KM_5 -> 5.0;
      case KM_10 -> 10.0;
      default -> 0.0;
    };
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
        .course(ghostResult.getCourse())
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

    BigDecimal min = null;
    BigDecimal max = null;

    if (filterType != null) {
      switch (filterType) {
        case UNDER_3 -> {
          max = BigDecimal.valueOf(3.0);
        }
        case BETWEEN_3_5 -> {
          min = BigDecimal.valueOf(3.0);
          max = BigDecimal.valueOf(5.0);
        }
        case BETWEEN_5_10 -> {
          min = BigDecimal.valueOf(5.0);
          max = BigDecimal.valueOf(10.0);
        }
        case OVER_10 -> {
          min = BigDecimal.valueOf(10.0);
        }
        case ALL -> {
        }
      }
    }

    Slice<RunningResult> resultSlice = runningResultRepository.findMySoloRecordsByDistance(
        user.getId(),
        RunStatus.COMPLETED,
        min,
        max,
        pageable
    );

    return resultSlice.map(RunningRecordResDto::from);
  }


  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }
}


package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.dto.res.MatchSessionDetailResDto;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.recruit.repository.RecruitUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : MatchService
 * @since : 2025-12-21 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchSessionService {

  private final RecruitRepository recruitRepository;
  private final UserRepository userRepository;
  private final MatchSessionRepository matchSessionRepository;
  private final RecruitUserRepository recruitUserRepository;
  private final SessionUserRepository sessionUserRepository;

  public MatchSessionDetailResDto getSessionDetail(Long sessionId, Long userId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // 세션 참여자만 조회 가능
    sessionUserRepository.findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

    List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);
    return MatchSessionDetailResDto.from(session, sessionUsers);
  }

  @Transactional
  public Long createOfflineSession(Long recruitId, UserDetails userDetails) {

    User user = userRepository.findByLoginId(userDetails.getUsername())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

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

}


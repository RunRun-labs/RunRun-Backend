package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.notification.NotificationResDto;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.entity.Notification;
import com.multi.runrunbackend.domain.notification.repository.NotificationRepository;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.recruit.repository.RecruitUserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
  private final MatchSessionRepository matchSessionRepository;
  private final RecruitUserRepository recruitUserRepository;
  private final SessionUserRepository sessionUserRepository;
  private final NotificationRepository notificationRepository;
  //  private final SimpMessagingTemplate messagingTemplate;

  @Transactional
  public Long createOfflineSession(Long recruitId, Long id) {

    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

    if (!recruit.getUser().getId().equals(id)) {
      throw new ForbiddenException(ErrorCode.UNAUTHORIZED_HOST);
    }

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

    List<RecruitUser> participants = recruitUserRepository.findAllByRecruitIdAndIsDeletedFalse(
        recruitId);

    List<SessionUser> sessionUsers = participants.stream()
        .map(p -> SessionUser.builder()
            .matchSession(matchSession)
            .user(p.getUser())
            .isReady(false)
            .build())
        .collect(Collectors.toList());

    sessionUserRepository.saveAll(sessionUsers);

    //    notifyParticipants(sessionUsers, matchSession.getId());

    recruit.delete();

    return matchSession.getId();
  }

  private void notifyParticipants(List<SessionUser> users, Long sessionId) {
    for (SessionUser user : users) {
      Notification notification = Notification.builder()
          .receiver(user.getUser())
          .title("매칭 확정")
          .message("매칭이 확정되었습니다! 채팅방으로 이동하세요.")
          .notificationType(NotificationType.MATCH) // type
          .relatedType(RelatedType.ROOM)
          .relatedId(sessionId)
          .build();

      notificationRepository.save(notification);

      NotificationResDto msg = NotificationResDto.from(notification);

      //      messagingTemplate.convertAndSend("/sub/user/" + user.getUser().getId(), msg);
    }
  }
}

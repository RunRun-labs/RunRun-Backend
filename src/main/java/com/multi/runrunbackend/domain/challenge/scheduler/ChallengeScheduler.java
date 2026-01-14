package com.multi.runrunbackend.domain.challenge.scheduler;

import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import com.multi.runrunbackend.domain.challenge.repository.ChallengeRepository;
import com.multi.runrunbackend.domain.challenge.repository.UserChallengeRepository;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.service.NotificationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 매일 자정에 실행되어 종료된 챌린지를 확인하고, 완료하지 못한 참가자들을 실패 처리하는 스케줄러 조건: 챌린지 종료일 < 어제 (즉, 종료일이 지난
 * 경우) 대상: 상태가 JOINED 또는 IN_PROGRESS인 UserChallenge
 * @filename : ChallengeScheduler
 * @since : 25. 12. 23. 오후 3:38 화요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

  private final UserChallengeRepository userChallengeRepository;
  private final ChallengeRepository challengeRepository;
  private final NotificationService notificationService;


  /**
   * 매일 자정(00:00:00)에 실행 1. 시작일이 된 챌린지: JOINED -> IN_PROGRESS 변경 2. 종료된 챌린지: IN_PROGRESS -> FAILED
   * 변경 3. 종료된 챌린지 소프트 삭제
   */

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void scheduleChallengeStatus() {
    LocalDate today = LocalDate.now();

    updateStatusToInProgress(today);
    checkFailedChallenges(today);
    softDeleteExpiredChallenges(today);
  }

  private void updateStatusToInProgress(LocalDate today) {
    //챌린지 시작일 <= 오늘 AND 상태 == JOINED
    List<UserChallenge> startingChallenges = userChallengeRepository.findChallengesToStart(
        UserChallengeStatus.JOINED,
        today
    );

    Map<Challenge, List<UserChallenge>> challengesByChallenge = startingChallenges.stream()
        .collect(Collectors.groupingBy(UserChallenge::getChallenge));

    int count = 0;
    for (Map.Entry<Challenge, List<UserChallenge>> entry : challengesByChallenge.entrySet()) {
      Challenge challenge = entry.getKey();
      List<UserChallenge> participants = entry.getValue();

      for (UserChallenge uc : participants) {
        uc.startProgress(); // 상태를 IN_PROGRESS로 변경
        count++;
      }

      try {
        for (UserChallenge uc : participants) {
          try {
            notificationService.create(
                uc.getUser(),
                "챌린지 시작",
                challenge.getTitle() + " 챌린지가 시작되었습니다.",
                NotificationType.CHALLENGE,
                RelatedType.CHALLENGE,
                challenge.getId()
            );
            log.debug("챌린지 시작 알림 발송 완료 - challengeId: {}, receiverId: {}",
                challenge.getId(), uc.getUser().getId());
          } catch (Exception e) {
            log.error("챌린지 시작 알림 생성 실패 - challengeId: {}, receiverId: {}",
                challenge.getId(), uc.getUser().getId(), e);
            // 개별 실패는 전체 알림 발송을 중단하지 않음
          }
        }
        log.info("챌린지 시작 알림 발송 완료 - challengeId: {}, title: {}, 참가자 수: {}",
            challenge.getId(), challenge.getTitle(), participants.size());
      } catch (Exception e) {
        log.error("챌린지 시작 알림 발송 중 오류 발생 - challengeId: {}, title: {}",
            challenge.getId(), challenge.getTitle(), e);
      }
    }

    if (count > 0) {
      log.info("Started {} challenges (JOINED -> IN_PROGRESS).", count);
    }
  }

  private void checkFailedChallenges(LocalDate today) {
    List<UserChallenge> activeParticipations = userChallengeRepository.findChallengesToFail(
        List.of(UserChallengeStatus.JOINED, UserChallengeStatus.IN_PROGRESS),
        today
    );

    Map<Challenge, List<UserChallenge>> challengesByChallenge = activeParticipations.stream()
        .collect(Collectors.groupingBy(UserChallenge::getChallenge));

    int count = 0;
    for (Map.Entry<Challenge, List<UserChallenge>> entry : challengesByChallenge.entrySet()) {
      Challenge challenge = entry.getKey();
      List<UserChallenge> participants = entry.getValue();

      for (UserChallenge uc : participants) {
        uc.fail();
        count++;
      }

      try {
        for (UserChallenge uc : participants) {
          try {
            notificationService.create(
                uc.getUser(),
                "챌린지 실패",
                challenge.getTitle() + " 챌린지가 종료되었습니다.",
                NotificationType.CHALLENGE,
                RelatedType.CHALLENGE_END,
                challenge.getId()
            );
            log.debug("챌린지 실패 알림 발송 완료 - challengeId: {}, receiverId: {}",
                challenge.getId(), uc.getUser().getId());
          } catch (Exception e) {
            log.error("챌린지 실패 알림 생성 실패 - challengeId: {}, receiverId: {}",
                challenge.getId(), uc.getUser().getId(), e);
          }
        }
        log.info("챌린지 실패 알림 발송 완료 - challengeId: {}, title: {}, 실패 참가자 수: {}",
            challenge.getId(), challenge.getTitle(), participants.size());
      } catch (Exception e) {
        log.error("챌린지 실패 알림 발송 중 오류 발생 - challengeId: {}, title: {}",
            challenge.getId(), challenge.getTitle(), e);
      }
    }

    if (count > 0) {
      log.info("Processed {} failed challenge participations.", count);
    }
  }

  private void softDeleteExpiredChallenges(LocalDate today) {
    List<Challenge> expiredChallenges = challengeRepository.findExpiredChallenges(today);
    for (Challenge challenge : expiredChallenges) {
      challenge.deleteChallenge();
    }
    if (!expiredChallenges.isEmpty()) {
      log.info("Soft-deleted {} expired challenges.", expiredChallenges.size());
    }
  }
}

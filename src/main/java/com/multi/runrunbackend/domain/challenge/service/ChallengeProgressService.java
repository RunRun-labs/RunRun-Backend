package com.multi.runrunbackend.domain.challenge.service;

import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import com.multi.runrunbackend.domain.challenge.repository.UserChallengeRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.service.NotificationService;
import com.multi.runrunbackend.domain.point.service.PointService;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 러닝 종료시 챌린지 진행도 처리를 위한 서비스
 * @filename : ChallengeProgressService
 * @since : 26. 1. 6. 오후 2:04 화요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeProgressService {

    private final UserChallengeRepository userChallengeRepository;
    private final NotificationService notificationService;
    private final PointService pointService;

    /**
     * 러닝 종료 시 챌린지 진행도 반영
     *
     */
    @Transactional // 여기에 트랜잭션 경계 설정
    public void applyRunningResult(RunningResult runningResult) {

        if (runningResult.getRunStatus() != RunStatus.COMPLETED) {
            return;
        }

        User user = runningResult.getUser();
        LocalDate runDate = runningResult.getStartedAt().toLocalDate();

        List<UserChallenge> activeChallenges =
                userChallengeRepository.findByUserAndStatus(
                        user,
                        UserChallengeStatus.IN_PROGRESS
                );

        for (UserChallenge uc : activeChallenges) {
            Challenge challenge = uc.getChallenge();

            if (!isWithinChallengePeriod(challenge, runDate)) {
                continue;
            }

            UserChallengeStatus statusBefore = uc.getStatus();

            // 진행도 업데이트 전 값 저장
            double previousProgress = uc.getProgressValue();
            double targetValue = challenge.getTargetValue();
            boolean wasCompleted = (previousProgress >= targetValue);

            // 아래 private 메서드들은 이 메서드의 트랜잭션에 참여합니다.
            switch (challenge.getChallengeType()) {
                case DISTANCE -> applyDistanceChallenge(uc, runningResult);
                case TIME -> applyTimeChallenge(uc, runningResult);
                case COUNT -> applyCountChallenge(uc, runDate);
            }

            // 챌린지 완료 체크 및 포인트 적립 추가 -> 진행도 업데이트 후 값 확인
            double currentProgress = uc.getProgressValue();
            boolean isNowCompleted = (currentProgress >= targetValue);

            // 방금 완료된 경우에만 포인트 적립 (중복 방지)
            if (!wasCompleted && isNowCompleted) {
                pointService.earnPointsForChallengeSuccess(user.getId());
            }

            if (statusBefore == UserChallengeStatus.IN_PROGRESS
                    && uc.getStatus() == UserChallengeStatus.COMPLETED) {
                try {
                    notificationService.create(
                            uc.getUser(),
                            "챌린지 완료",
                            challenge.getTitle() + " 챌린지를 완료했습니다!",
                            NotificationType.CHALLENGE,
                            RelatedType.CHALLENGE_END,
                            challenge.getId()
                    );
                    log.info("챌린지 완료 알림 발송 완료 - challengeId: {}, userId: {}, title: {}",
                            challenge.getId(), uc.getUser().getId(), challenge.getTitle());
                } catch (Exception e) {
                    log.error("챌린지 완료 알림 생성 실패 - challengeId: {}, userId: {}, title: {}",
                            challenge.getId(), uc.getUser().getId(), challenge.getTitle(), e);
                    // 알림 발송 실패해도 진행도 업데이트는 유지
                }
            }
        }
    }


    /*
     * DISTANCE = 거리형
     */
    private void applyDistanceChallenge(
            UserChallenge uc,
            RunningResult rr
    ) {
        double distanceKm = rr.getTotalDistance().doubleValue();
        uc.updateProgress(uc.getProgressValue() + distanceKm);
    }

    /**
     * TIME = 시간형
     */
    private void applyTimeChallenge(
            UserChallenge uc,
            RunningResult rr
    ) {
        double minutes = rr.getTotalTime() / 60.0;
        uc.updateProgress(uc.getProgressValue() + minutes);
    }

    /**
     * COUNT = 출석형 (하루 1회만 인정)
     */
    private void applyCountChallenge(
            UserChallenge uc,
            LocalDate runDate
    ) {
        // 이미 오늘 반영했다면 무시
        if (runDate.equals(uc.getLastProgressDate())) {
            return;
        }

        uc.updateProgress(uc.getProgressValue() + 1);
        uc.setLastProgressDate(runDate);
    }

    /*
     * 러닝 날짜가 챌린지 기간 내에 있는지 확인
     * */
    private boolean isWithinChallengePeriod(
            Challenge challenge,
            LocalDate runDate
    ) {
        return !runDate.isBefore(challenge.getStartDate())
                && !runDate.isAfter(challenge.getEndDate());
    }
}
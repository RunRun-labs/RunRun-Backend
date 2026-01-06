package com.multi.runrunbackend.domain.challenge.service;

import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import com.multi.runrunbackend.domain.challenge.repository.UserChallengeRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeProgressService {

    private final UserChallengeRepository userChallengeRepository;

    /**
     * 러닝 종료 시 챌린지 진행도 반영
     */
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

            switch (challenge.getChallengeType()) {
                case DISTANCE -> applyDistanceChallenge(uc, runningResult);
                case TIME -> applyTimeChallenge(uc, runningResult);
                case COUNT -> applyCountChallenge(uc, runDate);
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
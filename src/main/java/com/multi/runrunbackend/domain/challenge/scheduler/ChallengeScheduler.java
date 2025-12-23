package com.multi.runrunbackend.domain.challenge.scheduler;

import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import com.multi.runrunbackend.domain.challenge.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 매일 자정에 실행되어 종료된 챌린지를 확인하고, 완료하지 못한 참가자들을 실패 처리하는 스케줄러
 * 조건: 챌린지 종료일 < 어제 (즉, 종료일이 지난 경우)
 * 대상: 상태가 JOINED 또는 IN_PROGRESS인 UserChallenge
 * @filename : ChallengeScheduler
 * @since : 25. 12. 23. 오후 3:38 화요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

    private final UserChallengeRepository userChallengeRepository;


    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초 실행
    @Transactional
    public void checkFailedChallenges() {
        log.info("Checking for failed challenges...");

        // 어제 날짜 기준으로 종료된 챌린지 찾기 (종료일 < 오늘)
        LocalDate today = LocalDate.now();

        List<UserChallenge> activeParticipations = userChallengeRepository.findExpiredChallenges(
                List.of(UserChallengeStatus.JOINED, UserChallengeStatus.IN_PROGRESS),
                today
        );

        int count = 0;
        for (UserChallenge uc : activeParticipations) {
            uc.fail(); // 상태를 FAILED로 변경
        }

    }
}

package com.multi.runrunbackend.domain.coupon.scheduler;

import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 체험 멤버십 만료 알림 스케줄러
 * @filename : ExperienceExpiryScheduler
 * @since : 2026. 1. 8. 수요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExperienceExpiryScheduler {

    private final MembershipRepository membershipRepository;
    // TODO 알림 서비스

    /**
     * 체험권 만료 3일 전 알림
     */
    @Scheduled(cron = "0 9 0 * * *") // 매일 오전 9시
    public void notifyExpiringMemberships() {
        LocalDateTime threeDaysLater = LocalDateTime.now().plusDays(3);

        List<Membership> expiring = membershipRepository
                .findByMembershipStatusAndNextBillingDateIsNullAndEndDateBetween(
                        MembershipStatus.ACTIVE,
                        LocalDateTime.now(),
                        threeDaysLater
                );

        for (Membership membership : expiring) {
            log.info("체험권 만료 예정 알림 - userId: {}", membership.getUser().getId());
        }
    }
}

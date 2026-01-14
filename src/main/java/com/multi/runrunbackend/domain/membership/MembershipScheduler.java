package com.multi.runrunbackend.domain.membership;

import com.multi.runrunbackend.domain.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : BoKyung
 * @description : 멤버십 스케줄러
 * @since : 25. 12. 31. 수요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MembershipScheduler {

    private final MembershipService membershipService;

    /**
     * @description : 멤버십 만료 처리 (스케줄러-> 매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredMemberships() {

        log.info("=== 멤버십 만료 처리 스케줄러 시작 ===");

        membershipService.processExpiredMemberships();

        log.info("=== 멤버십 만료 처리 스케줄러 종료 ===");
    }

    /**
     * @description : 멤버십 만료 하루 전 알림 발송 (스케줄러-> 매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void sendMembershipExpiryNotifications() {
        log.info("=== 멤버십 만료 전 알림 스케줄러 시작 ===");
        membershipService.sendMembershipExpiryNotifications();
        log.info("=== 멤버십 만료 전 알림 스케줄러 종료 ===");
    }

}

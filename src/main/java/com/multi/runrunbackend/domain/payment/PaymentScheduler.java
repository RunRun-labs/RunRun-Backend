package com.multi.runrunbackend.domain.payment;

import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 멤버십 자동결제 스케줄러
 * @filename : PaymentScheduler
 * @since : 2026. 1. 1.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final MembershipRepository membershipRepository;
    private final PaymentService paymentService;

    /**
     * @description : 자동결제 처리
     * @author : BoKyung
     * @since : 2026. 1. 1.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processAutoPayments() {
        log.info("=== 자동결제 스케줄러 시작 ===");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);

        // 오늘이 결제일인 활성 멤버십 조회
        List<Membership> memberships = membershipRepository
                .findByMembershipStatusAndNextBillingDateBetween(
                        MembershipStatus.ACTIVE,
                        now,
                        tomorrow
                );

        log.info("자동결제 대상: {}건", memberships.size());

        int successCount = 0;
        int failCount = 0;

        for (Membership membership : memberships) {
            try {
                paymentService.processAutoPayment(membership.getUser(), membership);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("자동결제 실패 - membershipId: {}", membership.getId(), e);
            }
        }

        log.info("=== 자동결제 완료 - 성공: {}건, 실패: {}건 ===", successCount, failCount);
    }
}

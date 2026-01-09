package com.multi.runrunbackend.domain.payment;

import com.multi.runrunbackend.domain.coupon.service.CouponIssueService;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.payment.constant.PaymentStatus;
import com.multi.runrunbackend.domain.payment.entity.Payment;
import com.multi.runrunbackend.domain.payment.repository.PaymentRepository;
import com.multi.runrunbackend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class PaymentScheduler {

    private final MembershipRepository membershipRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final CouponIssueService couponIssueService;

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

    /**
     * @description : 오래된 READY 상태 결제 자동 정리
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupOldReadyPayments() {
        log.info("=== READY 상태 결제 정리 시작 ===");

        // 30분 전 시간
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        // 30분 이상 된 READY 상태 결제 조회
        List<Payment> oldPayments = paymentRepository.findOldPaymentsByStatus(
                PaymentStatus.READY,
                cutoffTime
        );

        if (oldPayments.isEmpty()) {
            log.info("정리할 READY 상태 결제 없음");
            return;
        }

        log.info("정리 대상 READY 상태 결제: {}건", oldPayments.size());

        int cleanedCount = 0;
        int couponRecoveredCount = 0;

        for (Payment payment : oldPayments) {
            try {
                // READY → FAILED 변경
                payment.fail();
                cleanedCount++;

                // 쿠폰 복구
                if (payment.getCouponIssue() != null) {
                    try {
                        couponIssueService.cancelCouponUse(
                                payment.getCouponIssue().getId()
                        );
                        couponRecoveredCount++;
                    } catch (Exception e) {
                        log.error("쿠폰 복구 실패", e);
                    }
                }

            } catch (Exception e) {
                log.error("READY 결제 정리 실패 - paymentId: {}", payment.getId(), e);
            }
        }

        log.info("=== READY 상태 결제 정리 완료 - 정리: {}건, 쿠폰 복구: {}건 ===",
                cleanedCount, couponRecoveredCount);

    }
}

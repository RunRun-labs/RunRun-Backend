package com.multi.runrunbackend.common.listener;

import com.multi.runrunbackend.common.event.UserSignedUpEvent;
import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponAutoIssueListener
 * @since : 2025. 12. 29. Monday
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponAutoIssueListener {

    private final CouponIssueService couponIssueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSignedUp(UserSignedUpEvent event) {
        try {
            couponIssueService.issueAuto(event.userId(), CouponTriggerEvent.SIGNUP, null);
        } catch (Exception e) {
            // 회원가입은 성공해야 하니 자동쿠폰 실패는 보통 삼킴(로그/모니터링)
            log.warn("[CouponAutoIssue] signup auto issue failed. userId={} msg={}",
                event.userId(), e.getMessage(), e);
        }
    }
}

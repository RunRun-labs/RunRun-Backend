package com.multi.runrunbackend.common.listener;

import com.multi.runrunbackend.common.event.RunningResultCompletedEvent;
import com.multi.runrunbackend.common.event.UserLoginEvent;
import com.multi.runrunbackend.common.event.UserSignedUpEvent;
import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.service.CouponIssueService;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author : kyungsoo
 * @description : 쿠폰 자동 발급 이벤트 리스너 (회원가입, 생일, 첫 러닝, 거리 달성)
 * @filename : CouponAutoIssueListener
 * @since : 2025. 12. 29. Monday
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponAutoIssueListener {

    private final CouponIssueService couponIssueService;
    private final UserRepository userRepository;
    private final RunningResultRepository runningResultRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSignedUp(UserSignedUpEvent event) {
        try {
            // 회원가입 쿠폰 지급
            couponIssueService.issueAuto(event.userId(), CouponTriggerEvent.SIGNUP, null);

            // 생일 쿠폰 지급 (회원가입일이 생일인 경우)
            userRepository.findById(event.userId()).ifPresent(user -> {
                LocalDate birthDate = user.getBirthDate();
                LocalDate today = LocalDate.now();

                if (birthDate != null &&
                    birthDate.getMonth() == today.getMonth() &&
                    birthDate.getDayOfMonth() == today.getDayOfMonth()) {
                    try {
                        couponIssueService.issueAuto(event.userId(), CouponTriggerEvent.BIRTHDAY,
                            null);
                        log.info(
                            "[CouponAutoIssue] birthday coupon issued. userId={}, birthDate={}",
                            event.userId(), birthDate);
                    } catch (Exception e) {
                        log.warn("[CouponAutoIssue] birthday auto issue failed. userId={} msg={}",
                            event.userId(), e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            // 회원가입은 성공해야 하니 자동쿠폰 실패는 보통 삼킴(로그/모니터링)
            log.warn("[CouponAutoIssue] signup auto issue failed. userId={} msg={}",
                event.userId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRunningResultCompleted(RunningResultCompletedEvent event) {
        try {
            Long userId = event.userId();
            
            int totalDistanceMeters = event.totalDistance()
                .multiply(java.math.BigDecimal.valueOf(1000))
                .intValue();

            long completedCount = runningResultRepository.countByUserIdAndRunStatusAndIsDeletedFalse(
                userId, RunStatus.COMPLETED);
            long timeoutCount = runningResultRepository.countByUserIdAndRunStatusAndIsDeletedFalse(
                userId, RunStatus.TIME_OUT);
            if (completedCount + timeoutCount == 1) {
                couponIssueService.issueAuto(userId, CouponTriggerEvent.FIRST_RUNNING, null);
                log.info(
                    "[CouponAutoIssue] first running coupon issued. userId={}, completedCount={}, timeoutCount={}",
                    userId, completedCount, timeoutCount);
            }

            BigDecimal accumulatedDistanceMeters = runningResultRepository.sumTotalDistanceByUserId(
                userId);
            int accumulatedMeters = accumulatedDistanceMeters.intValue();

            // 모든 활성 거리 달성 쿠폰 조건값을 확인하여 발급
            couponIssueService.issueAutoForAccumulatedDistance(userId, accumulatedMeters);
            log.info(
                "[CouponAutoIssue] distance reached check completed. userId={}, accumulatedDistance={}m",
                userId, accumulatedMeters);

        } catch (Exception e) {
            log.warn("[CouponAutoIssue] running completed auto issue failed. userId={} msg={}",
                event.userId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLogin(UserLoginEvent event) {
        log.info("[CouponAutoIssue] onUserLogin called. userId={}", event.userId());
        try {
            // 생일 쿠폰 지급 (로그인일이 생일인 경우)
            userRepository.findById(event.userId()).ifPresent(user -> {
                LocalDate birthDate = user.getBirthDate();
                LocalDate today = LocalDate.now();

                log.info("[CouponAutoIssue] user found. userId={}, birthDate={}, today={}",
                    event.userId(), birthDate, today);

                if (birthDate != null &&
                    birthDate.getMonth() == today.getMonth() &&
                    birthDate.getDayOfMonth() == today.getDayOfMonth()) {
                    log.info("[CouponAutoIssue] birthday matches. Attempting to issue coupon. userId={}",
                        event.userId());
                    try {
                        couponIssueService.issueAuto(event.userId(), CouponTriggerEvent.BIRTHDAY,
                            null);
                        log.info(
                            "[CouponAutoIssue] birthday coupon issued on login. userId={}, birthDate={}",
                            event.userId(), birthDate);
                    } catch (Exception e) {
                        log.warn("[CouponAutoIssue] birthday auto issue failed on login. userId={} msg={}",
                            event.userId(), e.getMessage(), e);
                    }
                } else {
                    log.info("[CouponAutoIssue] birthday does not match. userId={}, birthMonth={}, birthDay={}, todayMonth={}, todayDay={}",
                        event.userId(),
                        birthDate != null ? birthDate.getMonth() : null,
                        birthDate != null ? birthDate.getDayOfMonth() : null,
                        today.getMonth(), today.getDayOfMonth());
                }
            });
        } catch (Exception e) {
            log.warn("[CouponAutoIssue] login auto issue failed. userId={} msg={}",
                event.userId(), e.getMessage(), e);
        }
    }
}

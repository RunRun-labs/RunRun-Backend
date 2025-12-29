package com.multi.runrunbackend.domain.coupon.scheduler;

import com.multi.runrunbackend.domain.coupon.service.CouponStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponStatusScheduler
 * @since : 2025. 12. 29. Monday
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStatusScheduler {

    private final CouponStatusService couponStatusService;


    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void syncCouponStatus() {
        int expired = couponStatusService.expire();
        int soldOut = couponStatusService.soldOut();
        int activated = couponStatusService.activate();

        if (expired + soldOut + activated > 0) {
            log.info("[CouponStatusScheduler] status synced: expired={}, soldOut={}, activated={}",
                expired, soldOut, activated);
        }
    }
}

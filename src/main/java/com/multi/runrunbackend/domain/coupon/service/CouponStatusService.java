package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.domain.coupon.respository.CouponStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponStatusService
 * @since : 2025. 12. 29. Monday
 */
@Service
@RequiredArgsConstructor
public class CouponStatusService {

    private final CouponStatusRepository couponStatusRepository;

    @Transactional
    public int expire() {
        return couponStatusRepository.bulkExpire();
    }

    @Transactional
    public int soldOut() {
        return couponStatusRepository.bulkSoldOut();
    }

    @Transactional
    public int activate() {
        return couponStatusRepository.bulkActivate();
    }

    @Transactional
    public int expireCouponIssues() {
        return couponStatusRepository.issuedBulkExpire();
    }
}
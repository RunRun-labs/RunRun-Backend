package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleCreateResDto;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import com.multi.runrunbackend.domain.coupon.respository.CouponRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleService
 * @since : 2025. 12. 29. Monday
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponRoleService {

    private final CouponRoleRepository couponRoleRepository;
    private final CouponRepository couponRepository;

    public CouponRoleCreateResDto createCouponRole(CouponRoleCreateReqDto req) {
        Coupon coupon = couponRepository.findById(req.getCouponId())
            .orElseThrow(() -> new NotFoundException(
                ErrorCode.COUPON_NOT_FOUND));

        if (req.getConditionValue() == null) {
            if (couponRoleRepository.existsByTriggerEventAndIsActiveTrueAndConditionValueIsNull(
                req.getTriggerEvent())) {
                throw new DuplicateException(ErrorCode.COUPON_ROLE_DUPLICATE);
            }
        } else {
            if (couponRoleRepository.existsByTriggerEventAndIsActiveTrueAndConditionValue(
                req.getTriggerEvent(), req.getConditionValue())) {
                throw new DuplicateException(ErrorCode.COUPON_ROLE_DUPLICATE);
            }
        }

        try {
            CouponRole savedCouponRole = couponRoleRepository.save(CouponRole.create(req, coupon));
            return CouponRoleCreateResDto.of(savedCouponRole.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("[CouponRole] save failed: {}", e.getMostSpecificCause().getMessage(), e);
            throw new DuplicateException(ErrorCode.COUPON_ROLE_DUPLICATE);
        }

    }
}

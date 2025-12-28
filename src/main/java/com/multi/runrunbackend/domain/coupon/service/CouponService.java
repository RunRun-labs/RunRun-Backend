package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponUpdateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponCreateResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponPageResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponUpdateResDto;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.respository.CouponRepository;
import com.multi.runrunbackend.domain.coupon.util.CouponCodeGenerator;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponService
 * @since : 2025. 12. 27. Saturday
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    @Value("${coupon.code.single.length}")
    private int singleCodeLength;

    private final CouponRepository couponRepository;
    private final CouponCodeGenerator couponCodeGenerator;

    @Transactional
    public CouponCreateResDto createCoupon(CouponCreateReqDto req) {

        String code = (req.getCodeType() == CouponCodeType.MULTI)
            ? req.getCode().trim().toUpperCase()
            : couponCodeGenerator.generate(singleCodeLength);

        try {
            Coupon saved = couponRepository.save(Coupon.create(req, code));
            return CouponCreateResDto.of(saved.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("[Coupon] save failed: {}", e.getMostSpecificCause().getMessage(), e);
            throw new DuplicateException(ErrorCode.COUPON_CODE_DUPLICATE);
        }
    }

    @Transactional
    public CouponUpdateResDto updateCoupon(CouponUpdateReqDto req, Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));

        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new ForbiddenException(ErrorCode.COUPON_NOT_DRAFT);
        }

        coupon.update(req);
        return CouponUpdateResDto.of(coupon.getId());
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));

        coupon.delete();
    }

    @Transactional(readOnly = true)
    public CouponPageResDto getCouponList(
        CouponStatus status,
        CouponCodeType codeType,
        CouponChannel channel,
        String keyword,
        LocalDateTime startFrom,
        LocalDateTime endTo,
        Pageable pageable
    ) {
        String safeKeyword =
            (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();

        Specification<Coupon> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (codeType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("codeType"), codeType));
        }
        if (channel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channel"), channel));
        }
        if (safeKeyword != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), "%" + safeKeyword + "%"),
                cb.like(cb.lower(root.get("code")), "%" + safeKeyword + "%")
            ));
        }
        if (startFrom != null) {
            spec = spec.and(
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), startFrom));
        }
        if (endTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endAt"), endTo));
        }

        return CouponPageResDto.of(couponRepository.findAll(spec, pageable));
    }
}
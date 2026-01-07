package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleCreateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleUpdateReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleCreateResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponRoleListReqDto;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import com.multi.runrunbackend.domain.coupon.respository.CouponRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponRoleRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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

    @Transactional(readOnly = true)
    public Page<CouponRoleListItemResDto> getCouponRoleList(CouponRoleListReqDto req,
        Pageable pageable) {

        Specification<CouponRole> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getCouponId() != null) {
                predicates.add(cb.equal(root.get("coupon").get("id"), req.getCouponId()));
            }

            if (req.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), req.getIsActive()));
            }

            if (req.getTriggerEvents() != null && !req.getTriggerEvents().isEmpty()) {
                predicates.add(root.get("triggerEvent").in(req.getTriggerEvents()));
            }

            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String kw = "%" + req.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), kw));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return couponRoleRepository.findAll(spec, pageable)
            .map(CouponRoleListItemResDto::from);
    }

    @Transactional
    public void updateCouponRole(CouponRoleUpdateReqDto req,
        Long couponRoleId) {

        CouponRole couponRole = couponRoleRepository.findById(couponRoleId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ROLE_NOT_FOUND));
        Coupon coupon = couponRepository.findById(req.getCouponId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));
        if (couponRole.getIsActive()) {
            throw new BadRequestException(ErrorCode.COUPON_ROLE_ACTIVE);
        }
        couponRole.update(req, coupon);

    }

    @Transactional
    public void deleteCouponRole(Long couponRoleId) {

        CouponRole couponRole = couponRoleRepository.findById(couponRoleId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ROLE_NOT_FOUND));
        if (couponRole.getIsActive()) {
            throw new BadRequestException(ErrorCode.COUPON_ROLE_ACTIVE);
        }
        couponRole.delete();

    }

    @Transactional
    public void setActive(Long couponRoleId, Boolean isActive) {
        CouponRole role = couponRoleRepository.findById(couponRoleId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ROLE_NOT_FOUND));
        role.isActive(isActive);

    }
}

package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueStatus;
import com.multi.runrunbackend.domain.coupon.constant.CouponStatus;
import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRedeemReqDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListReqDto;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import com.multi.runrunbackend.domain.coupon.respository.CouponIssueRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponRoleRepository;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : 자동발급은 절대 실패시키면 안 되는 부가 로직, 로그만 남기고 return이 일반적 예외처리 x
 * @filename : CouponIssueService
 * @since : 2025. 12. 29. Monday
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final UserRepository userRepository;
    private final CouponRoleRepository couponRoleRepository;
    private final CouponRepository couponRepository;


    @Transactional(readOnly = true)
    public CursorPage<CouponIssueListItemResDto> getCouponIssueList(CustomUser principal,
        CouponIssueListReqDto req) {
        User user = getUserOrThrow(principal);
        return couponIssueRepository.searchIssuedCoupons(user.getId(), req);
    }

    @Transactional(readOnly = true)
    public long getCouponCount(CustomUser principal) {
        User user = getUserOrThrow(principal);
        return couponIssueRepository.countByUserId(user.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueAuto(Long userId, CouponTriggerEvent event, Integer conditionValue) {

        User user = userRepository.getReferenceById(userId);

        List<CouponRole> roles = couponRoleRepository.findActiveRoles(event, conditionValue);
        if (roles.isEmpty()) {
            log.info("[CouponAuto] no active role. event={} cond={}", event, conditionValue);
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        for (CouponRole role : roles) {
            Long couponId = role.getCoupon().getId();
            try {
                issueOneAuto(user, couponId, now);
            } catch (Exception e) {
                log.error("[CouponAuto] fail userId={} couponId={}", userId, couponId, e);
            }
        }
    }

    private void issueOneAuto(User user, Long couponId, LocalDateTime now) {

        try {
            Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElse(null);

            if (coupon == null) {
                log.info("[CouponAuto] coupon not found. userId={} couponId={}", user.getId(),
                    couponId);
                return;
            }

            if (coupon.getStatus() != CouponStatus.ACTIVE) {
                log.debug("[CouponAuto] skip inactive. userId={} couponId={}", user.getId(),
                    couponId);
                return;
            }
            if (now.isBefore(coupon.getStartAt())) {
                log.debug("[CouponAuto] skip not started. userId={} couponId={} startAt={} now={}",
                    user.getId(), couponId, coupon.getStartAt(), now);
                return;
            }
            if (now.isAfter(coupon.getEndAt())) {
                log.debug("[CouponAuto] skip expired. userId={} couponId={} endAt={} now={}",
                    user.getId(), couponId, coupon.getEndAt(), now);
                return;
            }

            Integer q = coupon.getQuantity();
            if (q != null && q > 0 && coupon.getIssuedCount() >= q) {
                return;
            }

            couponIssueRepository.save(CouponIssue.createAuto(coupon, user));

            int updated = couponRepository.increaseIssuedCountAndMaybeSoldOut(couponId);
            if (updated == 0) {
                log.debug("[CouponAuto] skip soldout(or not updatable). userId={} couponId={}",
                    user.getId(), couponId);
            }

        } catch (DataIntegrityViolationException e) {
            log.info("[CouponAuto] already issued. userId={} couponId={}", user.getId(), couponId);
        } catch (Exception e) {
            // 자동은 어떤 예외도 밖으로 새면 안 됨
            log.error("[CouponAuto] fail. userId={} couponId={}", user.getId(), couponId, e);
        }
    }

    @Transactional
    public void createCouponIssue(CustomUser principal, CouponRedeemReqDto req) {
        User user = getUserOrThrow(principal);
        String code = req.getCode() == null ? null : req.getCode().trim();
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));

        issueManual(user, coupon);
    }

    @Transactional
    public void download(CustomUser principal, Long couponId) {
        User user = getUserOrThrow(principal);

        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));

        issueManual(user, coupon);
    }

    /**
     * 수동 발급 공통 처리 (다운로드/코드입력 공통)
     */

    private void issueManual(User user, Coupon coupon) {

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartAt())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_STARTED);
        }
        if (now.isAfter(coupon.getEndAt())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        if (couponIssueRepository.existsByCouponIdAndUserId(coupon.getId(), user.getId())) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {
            couponIssueRepository.save(CouponIssue.create(coupon, user));

            int updated = couponRepository.increaseIssuedCount(coupon.getId());
            if (updated == 0) {
                coupon.soldOut();
                throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            }

        } catch (DataIntegrityViolationException e) {

            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Transactional
    public void deleteCouponIssue(Long couponIssueId, CustomUser principal) {
        User user = getUserOrThrow(principal);

        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        if (!user.getId().equals(couponIssue.getUser().getId())) {
            throw new ForbiddenException(ErrorCode.COUPON_ISSUE_FORBIDDEN);
        }
        if (couponIssue.getStatus() != CouponIssueStatus.AVAILABLE) {
            throw new ForbiddenException(ErrorCode.COUPON_ISSUE_NOT_AVAILABLE);
        }

        couponIssue.delete();
    }

    private User getUserOrThrow(CustomUser principal) {
        if (principal == null || principal.getLoginId() == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByLoginId(principal.getLoginId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }


}
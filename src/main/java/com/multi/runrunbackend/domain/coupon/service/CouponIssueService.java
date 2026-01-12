package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
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
import com.multi.runrunbackend.domain.coupon.util.CouponCodeGenerator;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final CouponCodeGenerator couponCodeGenerator;


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

        // 거리 달성 쿠폰 중복 발급 방지: RUN_COUNT_REACHED이고 conditionValue가 있으면 이미 발급받았는지 확인
        if (event == CouponTriggerEvent.RUN_COUNT_REACHED && conditionValue != null) {
            boolean alreadyIssued = couponIssueRepository.existsByUserIdAndTriggerEventAndConditionValue(
                userId, event, conditionValue);
            if (alreadyIssued) {
                log.info("[CouponAuto] already issued for distance. userId={} event={} conditionValue={}",
                    userId, event, conditionValue);
                return;
            }
        }

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

    /**
     * 누적 거리 기준 거리 달성 쿠폰 발급
     * - 사용자의 누적 거리가 쿠폰 조건값과 일치하는 모든 쿠폰을 발급
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueAutoForAccumulatedDistance(Long userId, int accumulatedDistanceMeters) {
        User user = userRepository.getReferenceById(userId);

        // 모든 활성 거리 달성 쿠폰 역할 조회
        List<CouponRole> allRoles = couponRoleRepository.findAllActiveRolesByEvent(
            CouponTriggerEvent.RUN_COUNT_REACHED);

        if (allRoles.isEmpty()) {
            log.debug("[CouponAuto] no active distance roles. userId={}", userId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 각 역할의 conditionValue가 누적 거리와 일치하는 경우 쿠폰 발급
        for (CouponRole role : allRoles) {
            Integer conditionValue = role.getConditionValue();
            if (conditionValue == null) {
                continue; // conditionValue가 없으면 건너뜀
            }

            // 누적 거리가 조건값과 정확히 일치하는 경우만 발급
            if (accumulatedDistanceMeters == conditionValue) {
                // 이미 발급받았는지 확인
                boolean alreadyIssued = couponIssueRepository.existsByUserIdAndTriggerEventAndConditionValue(
                    userId, CouponTriggerEvent.RUN_COUNT_REACHED, conditionValue);
                if (alreadyIssued) {
                    log.debug("[CouponAuto] already issued for accumulated distance. userId={} distance={}m",
                        userId, conditionValue);
                    continue;
                }

                Long couponId = role.getCoupon().getId();
                try {
                    issueOneAuto(user, couponId, now);
                    log.info("[CouponAuto] issued coupon for accumulated distance. userId={} distance={}m couponId={}",
                        userId, conditionValue, couponId);
                } catch (Exception e) {
                    log.error("[CouponAuto] fail userId={} couponId={} distance={}m", userId, couponId,
                        conditionValue, e);
                }
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

            String issueCode = couponCodeGenerator.generate(16);
            couponIssueRepository.save(CouponIssue.createAuto(coupon, user, issueCode));

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
            String issueCode = couponCodeGenerator.generate(16);
            couponIssueRepository.save(CouponIssue.create(coupon, user, issueCode));

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

    /**
     * 결제에 사용할 쿠폰 유효성 검증 및 조회
     */
    @Transactional(readOnly = true)
    public CouponIssue validateCouponForPayment(Long userId, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new NotFoundException(ErrorCode.COUPON_NOT_FOUND);
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));

        if (coupon.getBenefitType() != CouponBenefitType.FIXED_DISCOUNT
                && coupon.getBenefitType() != CouponBenefitType.RATE_DISCOUNT
                && coupon.getBenefitType() != CouponBenefitType.EXPERIENCE) {
            throw new BusinessException(ErrorCode.COUPON_NOT_APPLICABLE_FOR_PAYMENT);
        }

        CouponIssue couponIssue = couponIssueRepository
                .findByCouponAndUser(coupon, userRepository.getReferenceById(userId))
                .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        if (couponIssue.getStatus() != CouponIssueStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COUPON_ISSUE_NOT_AVAILABLE);
        }

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

        if (couponIssue.getExpiryAt() != null && now.isAfter(couponIssue.getExpiryAt())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        return couponIssue;
    }

    /**
     * 할인 금액 계산
     */
    public Integer calculateDiscount(Coupon coupon, Integer originalAmount) {
        CouponBenefitType benefitType = coupon.getBenefitType();
        Integer benefitValue = coupon.getBenefitValue();

        if (benefitType == CouponBenefitType.FIXED_DISCOUNT) {
            return Math.min(benefitValue, originalAmount);

        } else if (benefitType == CouponBenefitType.RATE_DISCOUNT) {
            Integer discountAmount = (originalAmount * benefitValue) / 100;
            return Math.min(discountAmount, originalAmount);

        } else if (benefitType == CouponBenefitType.EXPERIENCE) {
            return originalAmount;

        } else {
            return 0;
        }
    }

    /**
     * 쿠폰 사용 처리 (결제 완료 후 호출)
     */
    @Transactional
    public void useCoupon(Long couponIssueId) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        if (couponIssue.getStatus() != CouponIssueStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COUPON_ISSUE_NOT_AVAILABLE);
        }

        couponIssue.use();
        log.info("쿠폰 사용 처리 완료 - couponIssueId: {}", couponIssueId);
    }

    /**
     * 쿠폰 사용 취소 (결제 취소 시 호출)
     */
    @Transactional
    public void cancelCouponUse(Long couponIssueId) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        if (couponIssue.getStatus() != CouponIssueStatus.USED) {
            log.warn("사용 상태가 아닌 쿠폰 취소 시도 - couponIssueId: {}, status: {}",
                    couponIssueId, couponIssue.getStatus());
            return;
        }

        couponIssue.cancelUse();
        log.info("쿠폰 사용 취소 완료 - couponIssueId: {}", couponIssueId);
    }
}
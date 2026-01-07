package com.multi.runrunbackend.domain.payment.service;

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.coupon.service.CouponIssueService;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.payment.client.TossPaymentClient;
import com.multi.runrunbackend.domain.payment.constant.PaymentMethod;
import com.multi.runrunbackend.domain.payment.constant.PaymentStatus;
import com.multi.runrunbackend.domain.payment.dto.req.*;
import com.multi.runrunbackend.domain.payment.dto.res.*;
import com.multi.runrunbackend.domain.payment.entity.Payment;
import com.multi.runrunbackend.domain.payment.repository.PaymentRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author : BoKyung
 * @description : 결제 관리 서비스
 * @filename : PaymentService
 * @since : 2026. 1. 1.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final Integer PREMIUM_MONTHLY_PRICE = 9900;
    private static final String PREMIUM_PLAN_NAME = "프리미엄 플랜";

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TossPaymentClient tossPaymentClient;
    private final CouponIssueService couponIssueService;

    /**
     * @description : 결제 요청 생성
     */
    @Transactional
    public PaymentRequestResDto createPaymentRequest(
            CustomUser principal,
            PaymentRequestReqDto req
    ) {
        User user = getUserOrThrow(principal);

        membershipRepository.findByUser(user).ifPresent(membership -> {
            if (membership.getMembershipStatus() == MembershipStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_PREMIUM);
            }
        });

        // 주문 ID 생성
        String orderId = "ORDER_" + UUID.randomUUID().toString();

        // 프리미엄 가격 = 9900원 고정
        Integer originalAmount = PREMIUM_MONTHLY_PRICE;

        // 할인 금액 계산 (쿠폰)
        Integer discountAmount = 0;
        CouponIssue couponIssue = null;

        // 쿠폰 적용
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            // 쿠폰 유효성 검증 및 조회
            couponIssue = couponIssueService.validateCouponForPayment(
                    user.getId(),
                    req.getCouponCode()
            );

            // 할인 금액 계산
            discountAmount = couponIssueService.calculateDiscount(
                    couponIssue.getCoupon(),
                    originalAmount
            );
        }

        // 최종 금액이 음수가 되지 않도록 체크
        Integer finalAmount = originalAmount - discountAmount;
        if (finalAmount < 0) {
            finalAmount = 0;
        }

        // Payment 엔티티 생성
        Payment payment = Payment.create(
                user,
                originalAmount,
                discountAmount,
                orderId,
                couponIssue
        );
        paymentRepository.save(payment);

        log.info("결제 요청 생성 - orderId: {}, 사용자: {}, 금액: {}원",
                orderId, user.getLoginId(), payment.getFinalAmount());

        return PaymentRequestResDto.builder()
                .orderId(orderId)
                .amount(payment.getFinalAmount())
                .orderName(PREMIUM_PLAN_NAME)
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .customerKey("USER_" + user.getId())
                .isFreePayment(finalAmount == 0)
                .build();
    }

    /**
     * @description : 결제 승인 (paymentKey 기반)
     */
    @Transactional
    public PaymentApproveResDto confirmPayment(CustomUser principal, PaymentApproveReqDto req) {
        User user = getUserOrThrow(principal);

        Payment payment = paymentRepository.findByOrderIdWithLock(req.getOrderId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 확인
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        // 중복 결제 체크
        if (payment.getPaymentStatus() == PaymentStatus.DONE) {
            log.error("중복 결제 감지 - orderId: {}", req.getOrderId());
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 금액 검증
        if (!payment.getFinalAmount().equals(req.getAmount())) {
            payment.cancelBySystem("결제 금액 불일치");
            if (payment.getCouponIssue() != null) {
                couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
            }

            log.error("금액 불일치 - orderId: {}, 요청: {}원, DB: {}원",
                    req.getOrderId(), req.getAmount(), payment.getFinalAmount());
            throw new BadRequestException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        try {
            // 토스 API 호출
            TossPaymentResDto tossResponse = tossPaymentClient.approvePayment(req);

            // 토스 응답 (DONE)
            if (!"DONE".equals(tossResponse.getStatus())) {
                payment.fail();

                if (payment.getCouponIssue() != null) {
                    couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                }

                log.error("토스 승인 실패 - orderId: {}, status: {}",
                        req.getOrderId(), tossResponse.getStatus());
                throw new ExternalApiException(ErrorCode.PAYMENT_APPROVAL_FAILED);
            }

            // 결제수단 매핑
            PaymentMethod paymentMethod = mapPaymentMethod(tossResponse.getMethod());

            // 빌링키 추출 (자동결제용) - 일단 null
            String billingKey = null;

            // Payment 완료 처리
            payment.complete(tossResponse.getPaymentKey(), paymentMethod, billingKey);

            // 쿠폰 사용 처리
            if (payment.getCouponIssue() != null) {
                couponIssueService.useCoupon(payment.getCouponIssue().getId());
            }

            // Membership 활성화
            Membership membership = membershipRepository.findByUser(user)
                    .orElseGet(() -> membershipRepository.save(Membership.create(user)));

            activateMembershipWithCoupon(membership, payment);

            log.info("결제 승인 완료 - orderId: {}, 금액: {}원, 수단: {}",
                    req.getOrderId(), payment.getFinalAmount(), paymentMethod);

            return PaymentApproveResDto.builder()
                    .orderId(payment.getOrderId())
                    .paymentKey(payment.getPaymentKey())
                    .amount(payment.getFinalAmount())
                    .status(payment.getPaymentStatus().name())
                    .approvedAt(payment.getApprovedAt().toString())
                    .build();

        } catch (ExternalApiException e) {
            payment.fail();

            // 예외 발생시 쿠폰 복구
            if (payment.getCouponIssue() != null) {
                try {
                    couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                } catch (Exception ex) {
                    log.error("쿠폰 복구 실패 - couponIssueId: {}",
                            payment.getCouponIssue().getId(), ex);
                }
            }

            throw e;
        } catch (Exception e) {
            payment.fail();

            if (payment.getCouponIssue() != null) {
                try {
                    couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                } catch (Exception ex) {
                    log.error("쿠폰 복구 실패 - couponIssueId: {}",
                            payment.getCouponIssue().getId(), ex);
                }
            }
            log.error("결제 승인 처리 중 오류 - orderId: {}", req.getOrderId(), e);
            throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    /**
     * @description : 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResDto> getPaymentHistory(
            CustomUser principal,
            Pageable pageable
    ) {
        User user = getUserOrThrow(principal);

        Page<Payment> payments = paymentRepository.findByUserOrderByCreatedAtDesc(
                user, pageable
        );

        return payments.map(PaymentHistoryResDto::fromEntity);
    }

    /**
     * @description : 토스 결제수단
     */
    private PaymentMethod mapPaymentMethod(String tossMethod) {
        if (tossMethod == null) {
            return PaymentMethod.CARD;
        }

        return switch (tossMethod.toUpperCase()) {
            case "CARD", "카드" -> PaymentMethod.CARD;
            case "VIRTUAL_ACCOUNT", "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT;
            case "TRANSFER", "계좌이체" -> PaymentMethod.TRANSFER;
            case "MOBILE_PHONE", "휴대폰" -> PaymentMethod.MOBILE_PHONE;
            case "CULTURE_GIFT_CERTIFICATE" -> PaymentMethod.CULTURE_GIFT_CERTIFICATE;
            case "BOOK_CULTURE_GIFT_CERTIFICATE" -> PaymentMethod.BOOK_CULTURE_GIFT_CERTIFICATE;
            default -> {
                log.warn("알 수 없는 결제수단: {}, CARD로 처리", tossMethod);
                yield PaymentMethod.CARD;
            }
        };
    }

    /**
     * @description : 사용자 조회
     */
    private User getUserOrThrow(CustomUser principal) {
        if (principal == null || principal.getLoginId() == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * @description : 빌링키로 자동결제 (스케줄러에서 호출)
     */
    @Transactional
    public void processAutoPayment(User user, Membership membership) {
        log.info("자동결제 시작 - 사용자: {}", user.getLoginId());

        // 빌링키 조회
        Payment lastPayment = paymentRepository
                .findFirstByUserAndBillingKeyIsNotNullOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BILLING_KEY_NOT_FOUND));

        String billingKey = lastPayment.getBillingKey();

        // 주문 ID 생성
        String orderId = "AUTO_" + UUID.randomUUID().toString();

        // Payment 엔티티 생성 (자동결제용)
        Integer originalAmount = PREMIUM_MONTHLY_PRICE;
        Payment payment = Payment.createForAutoPayment(
                user,
                originalAmount,
                0,
                orderId,
                billingKey
        );
        paymentRepository.save(payment);

        try {
            // 토스 빌링키 결제 API 호출
            TossBillingPaymentReqDto req = TossBillingPaymentReqDto.builder()
                    .customerKey(user.getId().toString())
                    .amount(originalAmount)
                    .orderId(orderId)
                    .orderName(PREMIUM_PLAN_NAME + " (자동결제)")
                    .customerEmail(user.getEmail())
                    .customerName(user.getName())
                    .build();

            TossPaymentResDto tossResponse = tossPaymentClient.payWithBillingKey(billingKey, req);

            if (!"DONE".equals(tossResponse.getStatus())) {
                payment.fail();
                log.error("자동결제 실패 - userId: {}", user.getId());
                throw new ExternalApiException(ErrorCode.BILLING_PAYMENT_FAILED);
            }

            // 결제수단 매핑
            PaymentMethod paymentMethod = mapPaymentMethod(tossResponse.getMethod());

            // Payment 완료 처리
            payment.complete(tossResponse.getPaymentKey(), paymentMethod, billingKey);

            // Membership 갱신
            membership.renew();

            log.info("자동결제 성공 - userId: {}, amount: {}원", user.getId(), originalAmount);

        } catch (Exception e) {
            payment.fail();

            // 멤버십 만료 처리
            membership.expire();

            log.error("자동결제 실패로 멤버십 만료 - userId: {}", user.getId(), e);
            throw new BusinessException(ErrorCode.BILLING_PAYMENT_FAILED);
        }
    }

    /**
     * @description : 무료 결제 승인 (0원 결제 - 쿠폰 적용)
     */
    @Transactional
    public PaymentApproveResDto confirmFreePayment(CustomUser principal, String orderId) {
        User user = getUserOrThrow(principal);

        Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 확인
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        // 0원 결제 검증
        if (payment.getFinalAmount() != 0) {
            log.error("무료 결제가 아님 - orderId: {}, amount: {}", orderId, payment.getFinalAmount());
            throw new BadRequestException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 중복 결제 체크
        if (payment.getPaymentStatus() == PaymentStatus.DONE) {
            log.error("중복 결제 감지 - orderId: {}", orderId);
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 무료 결제 완료 처리 (토스 API 호출 없음)
        payment.complete("FREE_" + orderId, PaymentMethod.CARD, null);

        // 쿠폰 사용 처리
        if (payment.getCouponIssue() != null) {
            couponIssueService.useCoupon(payment.getCouponIssue().getId());
        }

        // Membership 활성화
        Membership membership = membershipRepository.findByUser(user)
                .orElseGet(() -> membershipRepository.save(Membership.create(user)));

        activateMembershipWithCoupon(membership, payment);

        log.info("무료 결제 승인 완료 - orderId: {}, userId: {}", orderId, user.getId());

        return PaymentApproveResDto.builder()
                .orderId(payment.getOrderId())
                .paymentKey("FREE_" + orderId)
                .amount(payment.getFinalAmount())  // 실제 저장된 금액 사용
                .status(payment.getPaymentStatus().name())
                .approvedAt(payment.getApprovedAt().toString())
                .build();
    }

    /**
     * @description : 카드 자동결제(정기결제) 처리
     */
    @Transactional
    public PaymentApproveResDto confirmBillingFirstPayment(CustomUser principal, BillingFirstPaymentConfirmReqDto request) {

        User user = getUserOrThrow(principal);

        String orderId = request.getOrderId();
        String authKey = request.getAuthKey();
        String customerKey = request.getCustomerKey();
        Integer amount = request.getAmount();

        // 결제 요청(Payment) 조회
        Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 확인
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        // 중복 결제 방지
        if (payment.getPaymentStatus() == PaymentStatus.DONE) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 금액 검증
        if (!payment.getFinalAmount().equals(amount)) {
            payment.cancelBySystem("결제 금액 불일치");

            if (payment.getCouponIssue() != null) {
                couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
            }

            throw new BadRequestException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        try {
            // 빌링키 발급 (authKey + customerKey)
            TossBillingKeyIssueReqDto issueReq = TossBillingKeyIssueReqDto.builder()
                    .authKey(authKey)
                    .customerKey(customerKey)
                    .build();

            TossBillingKeyResDto billingRes = tossPaymentClient.issueBillingKey(issueReq);
            String billingKey = billingRes.getBillingKey();

            // 빌링키로 첫 결제 승인
            TossBillingPaymentReqDto billingPayReq = TossBillingPaymentReqDto.builder()
                    .customerKey(customerKey)
                    .amount(payment.getFinalAmount())
                    .orderId(payment.getOrderId())
                    .orderName(PREMIUM_PLAN_NAME + " (첫 결제)")
                    .customerEmail(user.getEmail())
                    .customerName(user.getName())
                    .build();

            TossPaymentResDto payRes = tossPaymentClient.payWithBillingKey(billingKey, billingPayReq);

            if (!"DONE".equals(payRes.getStatus())) {
                payment.fail();

                if (payment.getCouponIssue() != null) {
                    try {
                        couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                    } catch (Exception ex) {
                        log.error("쿠폰 복구 실패 - couponIssueId: {}",
                                payment.getCouponIssue().getId(), ex);
                    }
                }

                throw new ExternalApiException(ErrorCode.BILLING_PAYMENT_FAILED);
            }

            // DB payment 완료 처리 -> approvedAt 생김
            PaymentMethod paymentMethod = mapPaymentMethod(payRes.getMethod());
            payment.complete(payRes.getPaymentKey(), paymentMethod, billingKey);

            // 쿠폰 사용 처리 추가
            if (payment.getCouponIssue() != null) {
                couponIssueService.useCoupon(payment.getCouponIssue().getId());
            }

            // 멤버십 생성/활성화 (없으면 생성)
            Membership membership = membershipRepository.findByUser(user)
                    .orElseGet(() -> membershipRepository.save(Membership.create(user)));

            activateMembershipWithCoupon(membership, payment);

            return PaymentApproveResDto.builder()
                    .orderId(payment.getOrderId())
                    .paymentKey(payment.getPaymentKey())
                    .amount(payment.getFinalAmount())
                    .status(payment.getPaymentStatus().name())
                    .approvedAt(payment.getApprovedAt().toString())
                    .build();

        } catch (ExternalApiException e) {
            payment.fail();

            if (payment.getCouponIssue() != null) {
                try {
                    couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                } catch (Exception ex) {
                    log.error("쿠폰 복구 실패 - couponIssueId: {}",
                            payment.getCouponIssue().getId(), ex);
                }
            }
            throw e;
        } catch (Exception e) {
            payment.fail();

            if (payment.getCouponIssue() != null) {
                try {
                    couponIssueService.cancelCouponUse(payment.getCouponIssue().getId());
                } catch (Exception ex) {
                    log.error("쿠폰 복구 실패 - couponIssueId: {}",
                            payment.getCouponIssue().getId(), ex);
                }
            }
            log.error("빌링키 첫 결제 처리 중 오류 - orderId: {}", orderId, e);
            throw new BusinessException(ErrorCode.BILLING_PAYMENT_FAILED);
        }
    }

    /**
     * 쿠폰에 따른 멤버십 활성화 처리
     */
    private void activateMembershipWithCoupon(Membership membership, Payment payment) {
        if (payment.getCouponIssue() != null) {
            Coupon coupon = payment.getCouponIssue().getCoupon();

            // EXPERIENCE 쿠폰 = 체험형 (benefitValue가 일수)
            if (coupon.getBenefitType() == CouponBenefitType.EXPERIENCE) {
                membership.activateForDays(coupon.getBenefitValue());
                membership.setNextBillingDate(null);
                log.info("체험 멤버십 활성화 - {}일", coupon.getBenefitValue());
            } else if (coupon.getBenefitType() == CouponBenefitType.FIXED_DISCOUNT
                    || coupon.getBenefitType() == CouponBenefitType.RATE_DISCOUNT) {

                membership.reactivate();
            } else {

                membership.reactivate();
            }
        } else {

            membership.reactivate();
        }
    }

}

package com.multi.runrunbackend.domain.point.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointEarnReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointUseReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointMainResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointShopDetailResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointShopListResDto;
import com.multi.runrunbackend.domain.point.entity.PointExpiration;
import com.multi.runrunbackend.domain.point.entity.PointHistory;
import com.multi.runrunbackend.domain.point.entity.PointProduct;
import com.multi.runrunbackend.domain.point.entity.UserPoint;
import com.multi.runrunbackend.domain.point.repository.PointExpirationRepository;
import com.multi.runrunbackend.domain.point.repository.PointHistoryRepository;
import com.multi.runrunbackend.domain.point.repository.PointProductRepository;
import com.multi.runrunbackend.domain.point.repository.UserPointRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : BoKyung
 * @description : 포인트 서비스
 * @filename : PointService
 * @since : 2026. 01. 02. 금요일
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PointExpirationRepository pointExpirationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PointProductRepository pointProductRepository;
    private final FileStorage fileStorage;

    private static final int DAILY_LIMIT = 500;
    private static final double PREMIUM_MULTIPLIER = 1.5;

    /**
     * 포인트 메인 조회
     */
    public PointMainResDto getPointMain(Long userId) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseGet(() -> UserPoint.toEntity(getUserById(userId)));

        // 포인트 적립 방법 - 안내용
        List<PointMainResDto.PointEarnMethod> earnMethods = List.of(
                PointMainResDto.PointEarnMethod.builder()
                        .methodName("경기 참여")
                        .description("km 당 50P")
                        .earnAmount(50)
                        .build(),
                PointMainResDto.PointEarnMethod.builder()
                        .methodName("출석 체크")
                        .description("매일 출석 시")
                        .earnAmount(50)
                        .build(),
                PointMainResDto.PointEarnMethod.builder()
                        .methodName("친구 추천")
                        .description("친구 초대 성공 시")
                        .earnAmount(2000)
                        .build(),
                PointMainResDto.PointEarnMethod.builder()
                        .methodName("주간 미션")
                        .description("미션 완료 시")
                        .earnAmount(100)
                        .build()
        );

        // 소멸 예정 포인트
        LocalDateTime nextMonthEnd = LocalDateTime.now().plusMonths(1)
                .withDayOfMonth(1).minusDays(1);
        Integer expiringPoints = pointExpirationRepository.getExpiringPointsInPeriod(
                userId, LocalDateTime.now(), nextMonthEnd
        );

        PointMainResDto.UpcomingExpiryInfo expiryInfo =
                PointMainResDto.UpcomingExpiryInfo.builder()
                        .expiryDate(nextMonthEnd.format(
                                DateTimeFormatter.ofPattern("yyyy.MM dd일까지")))
                        .expiringPoints(expiringPoints)
                        .build();

        Integer earnedTotal = pointHistoryRepository.getTotalPointsByType(userId, "EARN");
        Integer usedTotal = pointHistoryRepository.getTotalPointsByType(userId, "USE");

        PointMainResDto.PointSummary summary = PointMainResDto.PointSummary.builder()
                .earnedPoints(earnedTotal)
                .usedPoints(usedTotal)
                .totalAccumulated(earnedTotal)
                .build();

        return PointMainResDto.builder()
                .availablePoints(userPoint.getTotalPoint())
                .earnMethods(earnMethods)
                .upcomingExpiry(expiryInfo)
                .summary(summary)
                .build();
    }

    /**
     * 포인트 내역 조회 (커서 기반 페이징)
     */
    public CursorPage<PointHistoryListResDto> getPointHistoryList(Long userId, PointHistoryListReqDto reqDto) {
        return pointHistoryRepository.searchPointHistory(reqDto, userId);
    }

    /**
     * 포인트 적립
     */
    @Transactional
    public void earnPoints(Long userId, PointEarnReqDto requestDto) {
        // 금액 유효성 검증
        if (requestDto.getAmount() == null || requestDto.getAmount() <= 0) {
            throw new BadRequestException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        if (requestDto.getReason() == null || requestDto.getReason().isBlank()) {
            throw new BadRequestException(ErrorCode.REASON_REQUIRED);
        }

        // 유효한 reason인지 검증
        List<String> validReasons = List.of(
                "RUNNING_COMPLETE", "ATTENDANCE", "INVITE", "WEEKLY_MISSION",
                "MONTHLY_MISSION", "LUCKY_BOX", "STREET_POINT", "EVENT"
        );

        if (!validReasons.contains(requestDto.getReason())) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT);
        }

        // 하루 적립 제한 제외 대상
        List<String> unlimitedReasons = List.of(
                "INVITE",           // 친구 초대 2000P
                "MONTHLY_MISSION",  // 월간 미션 1000P
                "EVENT"             // 이벤트 포인트
        );

        // 하루 적립 제한 체크
        if (!unlimitedReasons.contains(requestDto.getReason())) {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            Integer todayEarned = pointHistoryRepository.getTodayEarnedPoints(
                    userId, startOfDay, endOfDay
            );

            if (todayEarned + requestDto.getAmount() > DAILY_LIMIT) {
                throw new BusinessException(ErrorCode.DAILY_POINT_LIMIT_EXCEEDED);
            }
        }

        // 프리미엄 멤버십 확인 및 1.5배 적용
        boolean isPremium = checkPremiumMembership(userId);
        int finalAmount = requestDto.getAmount();
        if (isPremium) {
            finalAmount = (int) (requestDto.getAmount() * PREMIUM_MULTIPLIER);
        }

        User user = getUserById(userId);

        // UserPoint 적립
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPoint newPoint = UserPoint.toEntity(user);
                    return userPointRepository.save(newPoint);
                });

        userPoint.addPoint(finalAmount);

        // PointHistory 저장
        PointHistory history = PointHistory.toEntity(
                user, null, "EARN", finalAmount, requestDto.getReason()
        );
        pointHistoryRepository.save(history);

        // PointExpiration 저장 (1년 만료)
        PointExpiration expiration = PointExpiration.toEntity(user, history, finalAmount);
        pointExpirationRepository.save(expiration);
    }

    /**
     * 포인트 사용 (FIFO + 동시성 제어)
     */
    @Transactional
    public void usePoints(Long userId, PointUseReqDto requestDto) {
        // 금액 유효성 검증
        if (requestDto.getAmount() == null || requestDto.getAmount() <= 0) {
            throw new BadRequestException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        if (requestDto.getReason() == null || requestDto.getReason().isBlank()) {
            throw new BadRequestException(ErrorCode.REASON_REQUIRED);
        }

        // 유효한 reason인지 검증
        List<String> validReasons = List.of(
                "CREW_JOIN", "PRODUCT_EXCHANGE",
                "MEMBERSHIP_TRIAL", "LUCKY_BOX"
        );

        if (!validReasons.contains(requestDto.getReason())) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT);
        }

        User user = getUserById(userId);

        // 동시성 제어 - 비관적 락
        UserPoint userPoint = userPointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POINT_NOT_FOUND));

        // 잔액 확인
        if (userPoint.getTotalPoint() < requestDto.getAmount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // FIFO 순서로 포인트 차감
        int remainingAmount = requestDto.getAmount();
        List<PointExpiration> activePoints = pointExpirationRepository
                .findActivePointsByUserIdOrderByEarnedAt(userId);

        for (PointExpiration expiration : activePoints) {
            if (remainingAmount <= 0) break;

            int deductAmount = Math.min(remainingAmount, expiration.getRemainingPoint());
            expiration.usePoint(deductAmount);
            remainingAmount -= deductAmount;
        }

        // UserPoint 차감
        userPoint.subtractPoint(requestDto.getAmount());

        // PointHistory 저장
        PointProduct pointProduct = null;
        if (requestDto.getPointProductId() != null) {
            pointProduct = findProductById(requestDto.getPointProductId());

            // 상품이 판매 가능한지 확인
            if (!pointProduct.getIsAvailable()) {
                throw new BusinessException(ErrorCode.POINT_PRODUCT_NOT_AVAILABLE);
            }
        }

        PointHistory history = PointHistory.toEntity(
                user, pointProduct, "USE", requestDto.getAmount(), requestDto.getReason()
        );
        pointHistoryRepository.save(history);
    }

    /**
     * 크루 가입 시 포인트 차감 (동시성 제어)
     */
    @Transactional
    public void deductPointsForCrewJoin(Long userId, Integer amount, String reason) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        User user = getUserById(userId);

        UserPoint userPoint = userPointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POINT_NOT_FOUND));

        if (userPoint.getTotalPoint() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // FIFO 차감
        int remainingAmount = amount;
        List<PointExpiration> activePoints = pointExpirationRepository
                .findActivePointsByUserIdOrderByEarnedAt(userId);

        for (PointExpiration expiration : activePoints) {
            if (remainingAmount <= 0) break;

            int deductAmount = Math.min(remainingAmount, expiration.getRemainingPoint());
            expiration.usePoint(deductAmount);
            remainingAmount -= deductAmount;
        }

        userPoint.subtractPoint(amount);

        PointHistory history = PointHistory.toEntity(
                user, null, "USE", amount, reason
        );
        pointHistoryRepository.save(history);
    }

    /**
     * 포인트 상점 목록 조회
     */
    public PointShopListResDto getPointShop(Long userId) {

        Integer myPoints = userPointRepository.getTotalPointByUserId(userId);

        // 상품 목록 조회
        List<PointProduct> products = pointProductRepository
                .findByIsDeletedFalseAndIsAvailableTrue();

        // DTO 변환
        List<PointShopListResDto.ShopItemDto> productDtos = products.stream()
                .map(product -> {
                    // S3 key면 변환, 외부 URL이면 그대로
                    String imageUrl = resolveImageUrl(product.getProductImageUrl());

                    return PointShopListResDto.ShopItemDto.builder()
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .requiredPoint(product.getRequiredPoint())
                            .productImageUrl(imageUrl)
                            .canPurchase(myPoints >= product.getRequiredPoint())
                            .build();
                })
                .collect(Collectors.toList());

        return PointShopListResDto.builder()
                .myPoints(myPoints)
                .products(productDtos)
                .build();
    }

    /**
     * 포인트 상품 상세 조회
     */
    public PointShopDetailResDto getPointShopDetail(Long userId, Long productId) {

        PointProduct product = findProductById(productId);
        Integer myPoints = userPointRepository.getTotalPointByUserId(userId);

        // S3 URL 변환
        String imageUrl = resolveImageUrl(product.getProductImageUrl());

        return PointShopDetailResDto.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .requiredPoint(product.getRequiredPoint())
                .productImageUrl(imageUrl)
                .isAvailable(product.getIsAvailable())
                .myPoints(myPoints)
                .canPurchase(myPoints >= product.getRequiredPoint() && product.getIsAvailable())
                .build();
    }

    // ========================================
    // @description : 메서드 모음
    // ========================================

    /**
     * 사용자 조회
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 프리미엄 멤버십 확인
     */
    private boolean checkPremiumMembership(Long userId) {
        // 사용자 조회
        User user = getUserById(userId);

        // user로 Membership 조회
        return membershipRepository.findByUser(user)
                .map(membership -> membership.getMembershipStatus() == MembershipStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * 상품 조회
     */
    private PointProduct findProductById(Long productId) {
        return pointProductRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 이미지 URL 변환 (S3 key → HTTPS)
     */
    private String resolveImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }
        // S3 key면 HTTPS로 변환, 외부 URL이면 그대로
        if (!imageUrl.startsWith("http")) {
            return fileStorage.toHttpsUrl(imageUrl);
        }
        return imageUrl;
    }
}

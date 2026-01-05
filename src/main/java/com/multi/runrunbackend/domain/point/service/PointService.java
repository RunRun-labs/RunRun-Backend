package com.multi.runrunbackend.domain.point.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointEarnReqDto;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointMainResDto;
import com.multi.runrunbackend.domain.point.entity.PointExpiration;
import com.multi.runrunbackend.domain.point.entity.PointHistory;
import com.multi.runrunbackend.domain.point.entity.UserPoint;
import com.multi.runrunbackend.domain.point.repository.PointExpirationRepository;
import com.multi.runrunbackend.domain.point.repository.PointHistoryRepository;
import com.multi.runrunbackend.domain.point.repository.UserPointRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
}

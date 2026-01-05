package com.multi.runrunbackend.domain.point.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.point.dto.req.CursorPage;
import com.multi.runrunbackend.domain.point.dto.req.PointHistoryListReqDto;
import com.multi.runrunbackend.domain.point.dto.res.PointHistoryListResDto;
import com.multi.runrunbackend.domain.point.dto.res.PointMainResDto;
import com.multi.runrunbackend.domain.point.entity.UserPoint;
import com.multi.runrunbackend.domain.point.repository.PointExpirationRepository;
import com.multi.runrunbackend.domain.point.repository.PointHistoryRepository;
import com.multi.runrunbackend.domain.point.repository.UserPointRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}

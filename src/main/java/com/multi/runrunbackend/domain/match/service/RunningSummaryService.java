package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 러닝 요약 서비스 - 오늘 / 주간 러닝 요약 정보 제공
 * @filename : RunningSummaryService
 * @since : 26. 1. 4. 오후 5:15 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningSummaryService {

    private final UserRepository userRepository;
    private final RunningResultRepository runningResultRepository;

    /**
     * 오늘 러닝 요약
     */
    public TodaySummaryResult getTodaySummary(CustomUser principal) {
        User user = getUserByPrincipal(principal);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Object[]> rows =
                runningResultRepository.findTodaySummary(
                        user.getId(), start, end
                );

        BigDecimal distance = BigDecimal.ZERO;
        Integer time = 0;

        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            distance = (BigDecimal) row[0];
            time = ((Number) row[1]).intValue();
        }

        int calories = calculateCalories(distance, user.getWeightKg());

        return new TodaySummaryResult(distance, time, calories);
    }

    /**
     * 주간 러닝 요약
     */
    public WeeklySummaryResult getWeeklySummary(
            CustomUser principal,
            int weekOffset
    ) {
        User user = getUserByPrincipal(principal);

        LocalDate monday =
                LocalDate.now()
                        .with(DayOfWeek.MONDAY)
                        .plusWeeks(weekOffset);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = monday.plusDays(6).atTime(23, 59, 59);

        List<Object[]> rows =
                runningResultRepository.findWeeklySummary(
                        user.getId(), start, end
                );

        List<BigDecimal> dailyDistances =
                new ArrayList<>(Collections.nCopies(7, BigDecimal.ZERO));

        BigDecimal totalDistance = BigDecimal.ZERO;
        int totalTime = 0;

        for (Object[] row : rows) {
            int dayOfWeek = ((Number) row[0]).intValue(); // 1=일
            BigDecimal dist = (BigDecimal) row[1];
            int time = ((Number) row[2]).intValue();

            int index = (dayOfWeek + 5) % 7; // 월=0
            dailyDistances.set(index, dist);

            totalDistance = totalDistance.add(dist);
            totalTime += time;
        }

        return new WeeklySummaryResult(
                dailyDistances,
                totalDistance,
                totalTime
        );
    }


    private int calculateCalories(BigDecimal distanceKm, Integer weightKg) {
        if (distanceKm == null || weightKg == null) return 0;
        return distanceKm
                .multiply(BigDecimal.valueOf(weightKg))
                .multiply(BigDecimal.valueOf(1.036))
                .intValue();
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.USER_NOT_FOUND)
                );
    }

    /*
     *
     * */
    public record TodaySummaryResult(
            BigDecimal distance,
            Integer time,
            Integer calories
    ) {
    }

    public record WeeklySummaryResult(
            List<BigDecimal> dailyDistances,
            BigDecimal totalDistance,
            Integer totalTime
    ) {
    }
}

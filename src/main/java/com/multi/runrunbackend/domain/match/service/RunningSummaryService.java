package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.friend.repository.FriendRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.constant.ProfileVisibility;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserSetting;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.repository.UserSettingRepository;
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
public class RunningSummaryService {

    private final UserRepository userRepository;
    private final RunningResultRepository runningResultRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendRepository friendRepository;
    private final UserSettingRepository userSettingRepository;

    /**
     * 오늘 러닝 요약
     */
    @Transactional(readOnly = true)
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
     * 내 주간 러닝 요약
     */
    @Transactional(readOnly = true)
    public WeeklySummaryResult getWeeklySummary(
            CustomUser principal,
            int weekOffset
    ) {
        User me = getUserByPrincipal(principal);
        return getWeeklySummaryInternal(me, weekOffset);
    }

    /**
     * 타인 주간 러닝 요약
     */
    @Transactional(readOnly = true)
    public WeeklySummaryResult getWeeklySummaryByUser(
            Long userId,
            CustomUser principal,
            int weekOffset
    ) {
        User me = getUserByPrincipal(principal);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        validateProfileAccess(me, target);


        return getWeeklySummaryInternal(target, weekOffset);
    }

    /**
     * 공통 주간 집계 로직
     */
    private WeeklySummaryResult getWeeklySummaryInternal(
            User user,
            int weekOffset
    ) {
        LocalDate monday =
                LocalDate.now()
                        .with(DayOfWeek.MONDAY)
                        .plusWeeks(weekOffset);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = monday.plusDays(6).atTime(23, 59, 59);

        List<Object[]> rows =
                runningResultRepository.findWeeklySummary(
                        user.getId(),
                        start,
                        end
                );

        List<BigDecimal> dailyDistances =
                new ArrayList<>(Collections.nCopies(7, BigDecimal.ZERO));

        BigDecimal totalDistance = BigDecimal.ZERO;
        int totalTime = 0;

        for (Object[] row : rows) {
            int dayOfWeek = ((Number) row[0]).intValue(); // 0=일
            BigDecimal dist = (BigDecimal) row[1];
            int time = ((Number) row[2]).intValue();

            int index = (dayOfWeek + 6) % 7; // 월=0
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

    /*
     * 오늘 러닝 요약 결과 record
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

    /**
     * 프로필 접근 권한 검증
     */
    private void validateProfileAccess(User me, User target) {

        if (me.getId().equals(target.getId())) {
            return;
        }


        if (userBlockRepository.existsByBlockerAndBlockedUser(me, target)
                || userBlockRepository.existsByBlockerAndBlockedUser(target, me)) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        UserSetting setting =
                userSettingRepository.findByUserId(target.getId())
                        .orElse(UserSetting.createDefault(target));

        ProfileVisibility visibility = setting.getProfileVisibility();

        switch (visibility) {
            case PUBLIC -> {

            }

            case FRIENDS_ONLY -> {
                boolean isFriend =
                        friendRepository.findBetweenUsers(me, target)
                                .filter(Friend::isAccepted)
                                .isPresent();

                if (!isFriend) {
                    throw new ForbiddenException(ErrorCode.PROFILE_FRIENDS_ONLY);
                }
            }

            case PRIVATE -> {
                throw new ForbiddenException(ErrorCode.PROFILE_PRIVATE);
            }
        }
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
}

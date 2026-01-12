package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.constant.Tier;
import com.multi.runrunbackend.domain.match.dto.res.CourseStatsDto;
import com.multi.runrunbackend.domain.match.dto.res.HourlyStatsDto;
import com.multi.runrunbackend.domain.match.dto.res.MonthlyTrendDto;
import com.multi.runrunbackend.domain.match.dto.res.PaceDistributionDto;
import com.multi.runrunbackend.domain.match.dto.res.RunningStatsResDto;
import com.multi.runrunbackend.domain.match.dto.res.RunningTypeBreakdownDto;
import com.multi.runrunbackend.domain.match.dto.res.TierPaceDto;
import com.multi.runrunbackend.domain.match.dto.res.TierStatsDto;
import com.multi.runrunbackend.domain.match.dto.res.WeeklyTrendDto;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.RunningStatsRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : 러닝 통계 서비스
 * @filename : RunningStatsService
 * @since : 2026. 1. 15. Wednesday
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningStatsService {

    private final RunningStatsRepository runningStatsRepository;
    private final RunningResultRepository runningResultRepository;
    private final MatchSessionRepository matchSessionRepository;
    private final SessionUserRepository sessionUserRepository;
    private final UserRepository userRepository;

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double calculatePercentage(long part, long total) {
        return total > 0 ? (part * 100.0) / total : 0.0;
    }

    public RunningStatsResDto getRunningStats() {
        // 1. 기본 통계
        List<Object[]> ratesList = runningStatsRepository.calculateMatchRates();
        Object[] rates = ratesList.isEmpty() ? new Object[]{0L, 0L, 0L, 0L} : ratesList.get(0);
        long successCount = toLong(rates[0]);
        long cancelCount = toLong(rates[1]);
        long dropoutCount = toLong(rates[2]);
        long totalCount = toLong(rates[3]);

        double successRate = calculatePercentage(successCount, totalCount);
        double cancelRate = calculatePercentage(cancelCount, totalCount);
        double dropoutRate = calculatePercentage(dropoutCount, totalCount);

        // 매칭 평균 시간
        Double avgDuration = matchSessionRepository.calculateAvgMatchDuration();
        double avgMatchDuration = avgDuration != null ? avgDuration : 0.0;

        // 러닝 평균 거리
        BigDecimal avgDistance = runningStatsRepository.calculateAvgRunningDistance();
        BigDecimal avgRunningDistance = avgDistance != null ? avgDistance : BigDecimal.ZERO;

        // 총 뛴 거리 수
        BigDecimal totalDistance = runningStatsRepository.calculateTotalRunningDistance();

        // 총 러닝 횟수
        long totalRunningCount = runningStatsRepository.countTotalRunning();

        // 총 시간
        long totalRunningTime = runningStatsRepository.calculateTotalRunningTime();

        // 연속 러닝일
        Long maxConsecutiveDays = runningStatsRepository.findMaxConsecutiveDays();
        long consecutiveDays = maxConsecutiveDays != null ? maxConsecutiveDays : 0L;

        // 매칭 참여 비율
        long participantCount = sessionUserRepository.countDistinctParticipants();
        long totalUsers = userRepository.count();
        double participationRate = calculatePercentage(participantCount, totalUsers);

        // 2. 티어별 통계
        List<TierStatsDto> tierStats = calculateTierStats();

        // 3. 타입별 분포
        List<RunningTypeBreakdownDto> typeBreakdown = calculateTypeBreakdown(totalCount);

        // 4. 시간대별 통계
        List<HourlyStatsDto> hourlyStats = calculateHourlyStats();

        // 5. 티어별 평균 페이스
        List<TierPaceDto> tierPace = calculateTierPace();

        // 6. 주별 추이
        List<WeeklyTrendDto> weeklyTrend = calculateWeeklyTrend();

        // 7. 월별 추이
        List<MonthlyTrendDto> monthlyTrend = calculateMonthlyTrend();

        // 8. 페이스 분포
        List<PaceDistributionDto> paceDistribution = calculatePaceDistribution();

        // 9. 코스별 통계
        List<CourseStatsDto> courseStats = calculateCourseStats();

        return RunningStatsResDto.builder()
            .matchSuccessRate(successRate)
            .matchCancelRate(cancelRate)
            .runningDropoutRate(dropoutRate)
            .avgMatchDuration(avgMatchDuration)
            .avgRunningDistance(avgRunningDistance)
            .matchParticipationRate(participationRate)
            .totalRunningDistance(totalDistance)
            .totalRunningCount(totalRunningCount)
            .totalRunningTime(totalRunningTime)
            .maxConsecutiveDays(consecutiveDays)
            .tierStats(tierStats)
            .typeBreakdown(typeBreakdown)
            .hourlyStats(hourlyStats)
            .tierPaceStats(tierPace)
            .weeklyTrend(weeklyTrend)
            .monthlyTrend(monthlyTrend)
            .paceDistribution(paceDistribution)
            .courseStats(courseStats)
            .build();
    }

    private List<TierStatsDto> calculateTierStats() {
        List<Object[]> results = runningStatsRepository.sumByTier();
        return results.stream()
            .map(row -> {
                String tierName = (String) row[0];
                Tier tier = mapStringToTier(tierName);
                long count = toLong(row[1]);
                BigDecimal distance = toBigDecimal(row[2]);
                return TierStatsDto.builder()
                    .tier(tier)
                    .totalCount(count)
                    .totalDistance(distance)
                    .build();
            })
            .toList();
    }

    private Tier mapStringToTier(String tierName) {
        return switch (tierName) {
            case "거북이" -> Tier.거북이;
            case "토끼" -> Tier.토끼;
            case "사슴" -> Tier.사슴;
            case "표범" -> Tier.표범;
            case "호랑이" -> Tier.호랑이;
            case "장산범" -> Tier.장산범;
            default -> Tier.거북이; // 기본값
        };
    }

    private List<RunningTypeBreakdownDto> calculateTypeBreakdown(long totalCount) {
        List<Object[]> results = runningStatsRepository.countByRunningType();
        long total = results.stream().mapToLong(row -> toLong(row[1])).sum();
        final long finalTotal = total == 0 ? 1 : total; // 0으로 나누기 방지, effectively final로 만들기

        return results.stream()
            .map(row -> {
                RunningType type = (RunningType) row[0];
                long count = toLong(row[1]);
                double percentage = calculatePercentage(count, finalTotal);
                return RunningTypeBreakdownDto.builder()
                    .type(type)
                    .count(count)
                    .percentage(percentage)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
            .toList();
    }

    private List<HourlyStatsDto> calculateHourlyStats() {
        List<Object[]> results = runningStatsRepository.sumByHour();
        
        // 0-23시 모두 포함하도록 (없는 시간대는 0으로 채움)
        Map<Integer, Object[]> resultMap = results.stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue(),
                row -> row
            ));

        List<HourlyStatsDto> stats = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Object[] row = resultMap.get(hour);
            if (row != null) {
                stats.add(HourlyStatsDto.builder()
                    .hour(hour)
                    .count(toLong(row[1]))
                    .totalDistance(toBigDecimal(row[2]))
                    .build());
            } else {
                stats.add(HourlyStatsDto.builder()
                    .hour(hour)
                    .count(0L)
                    .totalDistance(BigDecimal.ZERO)
                    .build());
            }
        }
        return stats;
    }

    private List<TierPaceDto> calculateTierPace() {
        List<Object[]> results = runningStatsRepository.avgPaceByTier();
        return results.stream()
            .map(row -> {
                String tierName = (String) row[0];
                Tier tier = mapStringToTier(tierName);
                BigDecimal avgPace = toBigDecimal(row[1]);
                long sampleCount = toLong(row[2]);
                return TierPaceDto.builder()
                    .tier(tier)
                    .avgPace(avgPace)
                    .sampleCount(sampleCount)
                    .build();
            })
            .toList();
    }

    private List<WeeklyTrendDto> calculateWeeklyTrend() {
        List<Object[]> results = runningStatsRepository.sumWeeklyTrend();
        return results.stream()
            .map(row -> {
                LocalDate weekStart;
                if (row[0] instanceof LocalDate) {
                    weekStart = (LocalDate) row[0];
                } else if (row[0] instanceof java.sql.Date) {
                    weekStart = ((java.sql.Date) row[0]).toLocalDate();
                } else {
                    // String이나 다른 타입인 경우
                    weekStart = LocalDate.parse(row[0].toString());
                }
                long count = toLong(row[1]);
                BigDecimal distance = toBigDecimal(row[2]);
                long time = toLong(row[3]);
                return WeeklyTrendDto.builder()
                    .weekStart(weekStart)
                    .count(count)
                    .totalDistance(distance)
                    .totalTime(time)
                    .build();
            })
            .sorted((a, b) -> a.getWeekStart().compareTo(b.getWeekStart()))  // 오래된 순으로 정렬
            .toList();
    }

    private List<MonthlyTrendDto> calculateMonthlyTrend() {
        List<Object[]> results = runningStatsRepository.sumMonthlyTrend();
        return results.stream()
            .map(row -> {
                LocalDate monthDate;
                if (row[0] instanceof LocalDate) {
                    monthDate = (LocalDate) row[0];
                } else if (row[0] instanceof java.sql.Date) {
                    monthDate = ((java.sql.Date) row[0]).toLocalDate();
                } else {
                    // String이나 다른 타입인 경우
                    monthDate = LocalDate.parse(row[0].toString());
                }
                YearMonth month = YearMonth.from(monthDate);
                long count = toLong(row[1]);
                BigDecimal distance = toBigDecimal(row[2]);
                long time = toLong(row[3]);
                return MonthlyTrendDto.builder()
                    .month(month)
                    .count(count)
                    .totalDistance(distance)
                    .totalTime(time)
                    .build();
            })
            .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))  // 오래된 순으로 정렬
            .toList();
    }

    private List<PaceDistributionDto> calculatePaceDistribution() {
        List<Object[]> results = runningStatsRepository.distributionByPace();
        return results.stream()
            .map(row -> {
                String paceRange = (String) row[0];
                long userCount = toLong(row[1]);
                return PaceDistributionDto.builder()
                    .paceRange(paceRange)
                    .userCount(userCount)
                    .build();
            })
            .toList();
    }

    private List<CourseStatsDto> calculateCourseStats() {
        List<Object[]> results = runningStatsRepository.sumByCourse();
        long totalCount = results.stream().mapToLong(row -> toLong(row[2])).sum();
        final long finalTotalCount = totalCount == 0 ? 1 : totalCount; // 0으로 나누기 방지, effectively final로 만들기

        return results.stream()
            .map(row -> {
                Long courseId = toLong(row[0]);
                String courseName = (String) row[1];
                long usageCount = toLong(row[2]);
                double percentage = calculatePercentage(usageCount, finalTotalCount);
                return CourseStatsDto.builder()
                    .courseId(courseId)
                    .courseName(courseName != null ? courseName : "코스명 없음")
                    .usageCount(usageCount)
                    .percentage(percentage)
                    .build();
            })
            .toList();
    }
}

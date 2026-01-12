package com.multi.runrunbackend.domain.match.dto.res;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunningStatsResDto {
    // 기본 통계
    private double matchSuccessRate;        // 매칭 성공률 (COMPLETED + TIME_OUT)
    private double matchCancelRate;         // 매칭 취소율 (CANCELLED)
    private double runningDropoutRate;      // 러닝 중 이탈률 (GIVE_UP)
    private double avgMatchDuration;        // 매칭 평균 시간 (초)
    private BigDecimal avgRunningDistance;  // 러닝 평균 거리 (km)
    private double matchParticipationRate;  // 매칭 참여 비율 (%)
    
    // 추가 기본 통계
    private BigDecimal totalRunningDistance;  // 총 뛴 거리 수 (km)
    private long totalRunningCount;           // 총 러닝 횟수
    private long totalRunningTime;            // 총 시간 (초)
    private long maxConsecutiveDays;          // 연속 러닝일
    
    // 분포별 통계
    private List<TierStatsDto> tierStats;              // 티어별 러닝량
    private List<RunningTypeBreakdownDto> typeBreakdown; // 조건별 매칭 분포
    private List<HourlyStatsDto> hourlyStats;          // 시간대별 러닝량
    private List<TierPaceDto> tierPaceStats;           // 티어별 평균 페이스
    
    // 추가 통계
    private List<WeeklyTrendDto> weeklyTrend;          // 주별 추이 (거리, 시간, 횟수)
    private List<MonthlyTrendDto> monthlyTrend;        // 월별 추이 (거리, 시간, 횟수)
    private List<PaceDistributionDto> paceDistribution; // 유저 페이스 분포
    private List<CourseStatsDto> courseStats;          // 코스별 통계
}

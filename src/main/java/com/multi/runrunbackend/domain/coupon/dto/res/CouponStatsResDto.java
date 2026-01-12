package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponStatsResDto {
    private Long couponId;
    private StatsRange range;
    
    // 기간별 총합
    private long totalIssued;
    private long totalUsed;
    private long totalExpired;
    private long totalAvailable;
    private double usageRate;
    
    // 날짜별 추이
    private List<CouponStatsSeriesItemResDto> dailyTrend;
    
    // 상태별 분포
    private CouponStatusBreakdownResDto statusBreakdown;
    
    public static CouponStatsResDto of(
        Long couponId,
        StatsRange range,
        long totalIssued,
        long totalUsed,
        long totalExpired,
        long totalAvailable,
        List<CouponStatsSeriesItemResDto> dailyTrend,
        CouponStatusBreakdownResDto statusBreakdown
    ) {
        double usageRate = totalIssued > 0 ? (totalUsed * 100.0) / totalIssued : 0.0;
        return CouponStatsResDto.builder()
            .couponId(couponId)
            .range(range)
            .totalIssued(totalIssued)
            .totalUsed(totalUsed)
            .totalExpired(totalExpired)
            .totalAvailable(totalAvailable)
            .usageRate(usageRate)
            .dailyTrend(dailyTrend)
            .statusBreakdown(statusBreakdown)
            .build();
    }
}


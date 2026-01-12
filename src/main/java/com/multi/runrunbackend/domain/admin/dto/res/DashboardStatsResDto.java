package com.multi.runrunbackend.domain.admin.dto.res;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResDto {
    // 오늘의 광고
    private long todayAdClicks;
    private double activeAdPercentage;
    private long todayAdImpressions;
    private long newMembershipUsers;
    
    // 오늘의 쿠폰
    private long todayCouponIssued;
    private double todayCouponUsageRate;
    private long todayCouponUsed;
    private long newUsers;
    
    // Top 5 광고
    private List<TopAdResDto> topAds;
    
    // Top 5 쿠폰
    private List<TopCouponResDto> topCoupons;
    
    public static DashboardStatsResDto of(
        long todayAdClicks,
        double activeAdPercentage,
        long todayAdImpressions,
        long newMembershipUsers,
        long todayCouponIssued,
        double todayCouponUsageRate,
        long todayCouponUsed,
        long newUsers,
        List<TopAdResDto> topAds,
        List<TopCouponResDto> topCoupons
    ) {
        return DashboardStatsResDto.builder()
            .todayAdClicks(todayAdClicks)
            .activeAdPercentage(activeAdPercentage)
            .todayAdImpressions(todayAdImpressions)
            .newMembershipUsers(newMembershipUsers)
            .todayCouponIssued(todayCouponIssued)
            .todayCouponUsageRate(todayCouponUsageRate)
            .todayCouponUsed(todayCouponUsed)
            .newUsers(newUsers)
            .topAds(topAds)
            .topCoupons(topCoupons)
            .build();
    }
}


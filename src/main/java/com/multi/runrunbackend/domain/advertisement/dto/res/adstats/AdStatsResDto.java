package com.multi.runrunbackend.domain.advertisement.dto.res.adstats;

import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import com.multi.runrunbackend.domain.advertisement.dto.res.adslot.AdSlotBreakdownItemResDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdStatsResDto
 * @since : 2026. 1. 11. Sunday
 */

@Getter
@Builder
public class AdStatsResDto {

    private Long adId;
    private StatsRange range;

    // 기간별 통계 (AdDailyStats 기준)
    private long impressions;
    private long clicks;
    private double ctr;

    private List<AdStatsSeriesItemResDto> dailySeries;
    private List<AdSlotBreakdownItemResDto> slotBreakdown;

    public static AdStatsResDto of(
        Long adId,
        StatsRange range,
        long impressions,
        long clicks,
        List<AdStatsSeriesItemResDto> series,
        List<AdSlotBreakdownItemResDto> breakdown
    ) {
        double ctr = impressions <= 0 ? 0.0 : (clicks * 100.0) / impressions;
        return AdStatsResDto.builder()
            .adId(adId)
            .range(range)
            .impressions(impressions)
            .clicks(clicks)
            .ctr(ctr)
            .dailySeries(series)
            .slotBreakdown(breakdown)
            .build();
    }
}

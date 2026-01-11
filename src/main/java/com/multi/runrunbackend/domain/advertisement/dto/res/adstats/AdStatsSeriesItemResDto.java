package com.multi.runrunbackend.domain.advertisement.dto.res.adstats;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdStatsSeriesItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdStatsSeriesItemResDto {

    private LocalDate date;
    private long impressions;
    private long clicks;
    private double ctr;

    public static AdStatsSeriesItemResDto of(LocalDate date, long impressions, long clicks) {
        double ctr = impressions <= 0 ? 0.0 : (clicks * 100.0) / impressions;
        return AdStatsSeriesItemResDto.builder()
            .date(date)
            .impressions(impressions)
            .clicks(clicks)
            .ctr(ctr)
            .build();
    }
}

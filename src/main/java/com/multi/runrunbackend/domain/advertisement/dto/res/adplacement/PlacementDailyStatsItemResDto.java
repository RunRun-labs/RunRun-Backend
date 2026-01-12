package com.multi.runrunbackend.domain.advertisement.dto.res.adplacement;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : PlacementDailyStatsItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class PlacementDailyStatsItemResDto {

    private LocalDate statDate;
    private int impressions;
    private int clicks;

    public static PlacementDailyStatsItemResDto of(LocalDate statDate, int impressions,
        int clicks) {
        return PlacementDailyStatsItemResDto.builder()
            .statDate(statDate)
            .impressions(impressions)
            .clicks(clicks)
            .build();
    }
}

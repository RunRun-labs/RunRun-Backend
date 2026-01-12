package com.multi.runrunbackend.domain.advertisement.dto.res.adstats;

import com.multi.runrunbackend.domain.advertisement.entity.AdDailyStats;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdDailyStatsListItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdDailyStatsListItemResDto {

    private Long dailyStatsId;
    private LocalDate statDate;
    private Integer impressions;
    private Integer clicks;

    public static AdDailyStatsListItemResDto from(AdDailyStats s) {
        return AdDailyStatsListItemResDto.builder()
            .dailyStatsId(s.getId())
            .statDate(s.getStatDate())
            .impressions(s.getImpressions())
            .clicks(s.getClicks())
            .build();
    }
}

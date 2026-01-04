package com.multi.runrunbackend.domain.match.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 주간 러닝 요약 정보 응답 DTO
 * @filename : WeeklyRunningSummaryResDto
 * @since : 26. 1. 3. 오후 5:35 토요일
 */
@Getter
@AllArgsConstructor
public class WeeklyRunningSummaryResDto {

    private List<BigDecimal> dailyDistances;
    private BigDecimal totalDistanceKm;
    private Integer totalDurationSec;

    public static WeeklyRunningSummaryResDto from(
            List<BigDecimal> dailyDistances,
            BigDecimal totalDistanceKm,
            Integer totalDurationSec
    ) {
        return new WeeklyRunningSummaryResDto(
                dailyDistances, totalDistanceKm, totalDurationSec
        );
    }
}
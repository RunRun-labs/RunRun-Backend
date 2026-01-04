package com.multi.runrunbackend.domain.match.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 *
 * @author : kimyongwon
 * @description : 오늘의 러닝 요약 정보 응답 DTO
 * @filename : TodayRunningSummaryResDto
 * @since : 26. 1. 3. 오후 5:25 토요일
 */
@Getter
@AllArgsConstructor
public class TodayRunningSummaryResDto {

    private BigDecimal distanceKm;
    private Integer durationSec;
    private Integer calories;

    public static TodayRunningSummaryResDto from(
            BigDecimal distanceKm,
            Integer durationSec,
            Integer calories
    ) {
        return new TodayRunningSummaryResDto(distanceKm, durationSec, calories);
    }
}
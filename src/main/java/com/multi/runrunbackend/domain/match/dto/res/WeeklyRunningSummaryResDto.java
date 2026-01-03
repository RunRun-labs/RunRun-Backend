package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
 * @filename : WeeklyRunningSummaryResDto
 * @since : 26. 1. 3. 오후 5:35 토요일
 */
@Getter
@Builder
public class WeeklyRunningSummaryResDto {

    // 월~일 (index 0 = 월요일)
    private List<Double> dailyDistances;

    private double totalDistanceKm;
    private int totalDurationSec;
}
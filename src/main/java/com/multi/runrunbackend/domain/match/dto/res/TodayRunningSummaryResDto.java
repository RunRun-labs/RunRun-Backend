package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
 * @filename : TodayRunningSummaryResDto
 * @since : 26. 1. 3. 오후 5:25 토요일
 */
@Getter
@Builder
public class TodayRunningSummaryResDto {
    private double distanceKm;
    private int durationSec;
    private int calories;
}
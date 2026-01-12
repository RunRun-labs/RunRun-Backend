package com.multi.runrunbackend.domain.advertisement.dto.res.adslot;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotBreakdownItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdSlotBreakdownItemResDto {

    private AdSlotType slotType;
    private long impressions;
    private long clicks;
    private double ctr;

    public static AdSlotBreakdownItemResDto of(AdSlotType slotType, long impressions, long clicks) {
        double ctr = impressions <= 0 ? 0.0 : (clicks * 100.0) / impressions;
        return AdSlotBreakdownItemResDto.builder()
            .slotType(slotType)
            .impressions(impressions)
            .clicks(clicks)
            .ctr(ctr)
            .build();
    }
}

package com.multi.runrunbackend.domain.advertisement.dto.res.adslot;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotAdminListItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdSlotAdminListItemResDto {

    private Long slotId;
    private String name;
    private AdSlotType slotType;
    private Integer dailyLimit;
    private Boolean allowPremium;
    private AdSlotStatus status;

    public static AdSlotAdminListItemResDto from(AdSlot slot) {
        return AdSlotAdminListItemResDto.builder()
            .slotId(slot.getId())
            .name(slot.getName())
            .slotType(slot.getSlotType())
            .dailyLimit(slot.getDailyLimit())
            .allowPremium(slot.getAllowPremium())
            .status(slot.getStatus())
            .build();
    }
}
package com.multi.runrunbackend.domain.advertisement.dto.res.adslot;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.res.adplacement.AdPlacementAdminListItemResDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotAdminDetailResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdSlotAdminDetailResDto {

    private Long id;
    private String name;
    private AdSlotType slotType;
    private Integer dailyLimit;
    private Boolean allowPremium;
    private AdSlotStatus status;
    
    private List<AdPlacementAdminListItemResDto> placements;

    public static AdSlotAdminDetailResDto of(
        Long id,
        String name,
        AdSlotType slotType,
        Integer dailyLimit,
        Boolean allowPremium,
        AdSlotStatus status,
        List<AdPlacementAdminListItemResDto> placements
    ) {
        return AdSlotAdminDetailResDto.builder()
            .id(id)
            .name(name)
            .slotType(slotType)
            .dailyLimit(dailyLimit)
            .allowPremium(allowPremium)
            .status(status)
            .placements(placements)
            .build();
    }
}
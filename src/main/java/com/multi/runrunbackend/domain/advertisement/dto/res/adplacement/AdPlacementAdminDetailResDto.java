package com.multi.runrunbackend.domain.advertisement.dto.res.adplacement;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementAdminDetailResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdPlacementAdminDetailResDto {

    private Long id;

    private Long slotId;
    private String slotName;
    private AdSlotType slotType;

    private Long adId;
    private String adName;

    private Integer priority;
    private Integer weight;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private Boolean isActive;

    private Integer totalImpressions;
    private Integer totalClicks;

    public static AdPlacementAdminDetailResDto of(
        Long id,
        Long slotId,
        String slotName,
        AdSlotType slotType,
        Long adId,
        String adName,
        Integer weight,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isActive,
        Integer totalImpressions,
        Integer totalClicks
    ) {
        return AdPlacementAdminDetailResDto.builder()
            .id(id)
            .slotId(slotId)
            .slotName(slotName)
            .slotType(slotType)
            .adId(adId)
            .adName(adName)
            .weight(weight)
            .startAt(startAt)
            .endAt(endAt)
            .isActive(isActive)
            .totalImpressions(totalImpressions)
            .totalClicks(totalClicks)
            .build();
    }
}
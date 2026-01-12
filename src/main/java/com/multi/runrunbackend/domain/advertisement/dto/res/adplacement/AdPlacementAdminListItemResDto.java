package com.multi.runrunbackend.domain.advertisement.dto.res.adplacement;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminListItemProjection;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementAdminListItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdPlacementAdminListItemResDto {

    private Long placementId;

    private Long slotId;
    private String slotName;
    private AdSlotType slotType;

    private Long adId;
    private String adName;

    private Integer weight;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private Integer totalImpressions;
    private Integer totalClicks;

    private Boolean isActive;

    public static AdPlacementAdminListItemResDto from(AdPlacementAdminListItemProjection p) {
        return AdPlacementAdminListItemResDto.builder()
            .placementId(p.getPlacementId())
            .slotId(p.getSlotId())
            .slotName(p.getSlotName())
            .adId(p.getAdId())
            .adName(p.getAdName())
            .weight(p.getWeight())
            .startAt(p.getStartAt())
            .endAt(p.getEndAt())
            .totalImpressions(p.getTotalImpressions())
            .totalClicks(p.getTotalClicks())
            .isActive(p.getIsActive())
            .build();
    }
}
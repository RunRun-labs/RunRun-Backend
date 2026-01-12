package com.multi.runrunbackend.domain.advertisement.dto.res.ad;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminDetailResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdAdminDetailResDto {

    private Long id;
    private String name;
    private String imageUrl;
    private String redirectUrl;

    // Placement 통계
    private long totalPlacementCount;
    private long totalPlacementImpressions;
    private long totalPlacementClicks;
    private double totalPlacementCtr;

    public static AdAdminDetailResDto of(
        Long id,
        String name,
        String imageUrl,
        String redirectUrl,
        long totalPlacementCount,
        long totalPlacementImpressions,
        long totalPlacementClicks
    ) {
        double totalPlacementCtr = totalPlacementImpressions <= 0 ? 0.0
            : (totalPlacementClicks * 100.0) / totalPlacementImpressions;
        return AdAdminDetailResDto.builder()
            .id(id)
            .name(name)
            .imageUrl(imageUrl)
            .redirectUrl(redirectUrl)
            .totalPlacementCount(totalPlacementCount)
            .totalPlacementImpressions(totalPlacementImpressions)
            .totalPlacementClicks(totalPlacementClicks)
            .totalPlacementCtr(totalPlacementCtr)
            .build();
    }
}

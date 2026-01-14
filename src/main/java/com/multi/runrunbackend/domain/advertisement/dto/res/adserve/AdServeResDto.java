package com.multi.runrunbackend.domain.advertisement.dto.res.adserve;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdServeResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdServeResDto {

    private Long placementId;
    private Long adId;
    private String name;
    private String imageUrl;
    private String redirectUrl;

    public static AdServeResDto of(Long placementId, Long adId, String name, String imageUrl,
        String redirectUrl) {
        return AdServeResDto.builder()
            .placementId(placementId)
            .adId(adId)
            .name(name)
            .imageUrl(imageUrl)
            .redirectUrl(redirectUrl)
            .build();
    }

}

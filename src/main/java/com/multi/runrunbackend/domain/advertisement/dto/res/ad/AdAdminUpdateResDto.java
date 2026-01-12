package com.multi.runrunbackend.domain.advertisement.dto.res.ad;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminUpdateResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdAdminUpdateResDto {

    private Long adId;

    public static AdAdminUpdateResDto of(Long adId) {
        return AdAdminUpdateResDto.builder().adId(adId).build();
    }
}

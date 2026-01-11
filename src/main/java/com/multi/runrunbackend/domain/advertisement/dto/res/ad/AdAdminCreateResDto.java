package com.multi.runrunbackend.domain.advertisement.dto.res.ad;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminCreateResDto
 * @since : 2026. 1. 11. Sunday
 */

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdAdminCreateResDto {

    private Long adId;

    public static AdAdminCreateResDto of(Long adId) {
        return AdAdminCreateResDto.builder().adId(adId).build();
    }
}

package com.multi.runrunbackend.domain.advertisement.dto.res.ad;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminListItemResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Builder
public class AdAdminListItemResDto {

    private Long id;
    private String name;
    private String redirectUrl;

    public static AdAdminListItemResDto of(Long id, String name,
        String redirectUrl) {
        return AdAdminListItemResDto.builder()
            .id(id)
            .name(name)
            .redirectUrl(redirectUrl)
            .build();
    }
}

package com.multi.runrunbackend.domain.admin.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopAdResDto {
    private Long adId;
    private String adName;
    private long clicks;
    
    public static TopAdResDto of(Long adId, String adName, long clicks) {
        return TopAdResDto.builder()
            .adId(adId)
            .adName(adName)
            .clicks(clicks)
            .build();
    }
}


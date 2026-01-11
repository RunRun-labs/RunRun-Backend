package com.multi.runrunbackend.domain.advertisement.dto.req.adclick;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdImpressionReqDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
public class AdImpressionReqDto {

    @NotNull(message = "placementId는 필수입니다.")
    private Long placementId;
}

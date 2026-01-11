package com.multi.runrunbackend.domain.advertisement.dto.req.adclick;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdImpressionReqDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@Setter
@NoArgsConstructor
public class AdImpressionReqDto {

    @NotNull(message = "placementId는 필수입니다.")
    private Long placementId;
}

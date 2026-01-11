package com.multi.runrunbackend.domain.advertisement.dto.req.adplacement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementAdminUpdateReqDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
public class AdPlacementAdminUpdateReqDto {

    @NotNull(message = "slotId는 필수입니다.")
    private Long slotId;

    @NotNull(message = "adId는 필수입니다.")
    private Long adId;

    @NotNull(message = "weight는 필수입니다.")
    @Min(value = 1, message = "weight는 1 이상이어야 합니다.")
    private Integer weight;

    @NotNull(message = "startAt은 필수입니다.")
    private LocalDateTime startAt;

    @NotNull(message = "endAt은 필수입니다.")
    private LocalDateTime endAt;
}
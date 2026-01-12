package com.multi.runrunbackend.domain.advertisement.dto.req.adslot;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotAdminUpdateReqDto
 * @since : 2026. 1. 11. Sunday
 */

@Getter
public class AdSlotAdminUpdateReqDto {

    @NotBlank(message = "name은 필수입니다.")
    @Size(min = 2, max = 50, message = "name은 2~50자입니다.")
    private String name;

    @NotNull(message = "slotType은 필수입니다.")
    private AdSlotType slotType;

    @NotNull(message = "dailyLimit은 필수입니다. (0=무제한)")
    private Integer dailyLimit;

    @NotNull(message = "allowPremium은 필수입니다.")
    private Boolean allowPremium;
}

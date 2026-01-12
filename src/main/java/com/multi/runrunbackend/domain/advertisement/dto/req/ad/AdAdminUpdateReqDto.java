package com.multi.runrunbackend.domain.advertisement.dto.req.ad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminUpdateReqDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
public class AdAdminUpdateReqDto {

    @NotBlank(message = "name은 필수입니다.")
    @Size(min = 3, max = 50, message = "name은 3~50자입니다.")
    private String name;

    @NotBlank(message = "redirectUrl은 필수입니다.")
    private String redirectUrl;
}

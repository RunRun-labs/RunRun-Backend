package com.multi.runrunbackend.domain.crew.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 활동 생성 요청 DTO
 * @filename : CrewActivityCreateReqDto
 * @since : 25. 1. 11. 일요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "크루 활동 생성 요청")
public class CrewActivityCreateReqDto {

    @NotBlank(message = "지역은 필수입니다")
    @Schema(description = "활동 지역")
    private String region;

    @NotNull(message = "거리는 필수입니다")
    @Min(value = 1, message = "거리는 1km 이상이어야 합니다")
    @Schema(description = "활동 거리 (km)")
    private Integer distance;

    @NotNull(message = "참여 크루원 ID 목록은 필수입니다")
    @Schema(description = "참여 크루원 ID 목록")
    private List<Long> participantUserIds;
}

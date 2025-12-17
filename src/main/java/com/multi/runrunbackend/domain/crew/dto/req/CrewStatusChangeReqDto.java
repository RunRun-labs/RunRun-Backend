package com.multi.runrunbackend.domain.crew.dto.req;

import com.multi.runrunbackend.domain.crew.entity.CrewRecruitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 모집 상태 변경 요청 DTO (RECRUITING-모집중, CLOSED-모집마감)
 * @filename : CrewStatusChangeReqDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 모집 상태 변경 요청")
public class CrewStatusChangeReqDto {

    @Schema(description = "모집 상태", allowableValues = {"RECRUITING", "CLOSED"}, required = true)
    @NotNull(message = "모집 상태는 필수입니다.")
    private CrewRecruitStatus recruitStatus;
}

package com.multi.runrunbackend.domain.crew.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 생성 요청 DTO
 * @filename : CrewCreateReqDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 생성")
public class CrewCreateReqDto {

    @Schema(description = "크루명", required = true)
    @NotBlank(message = "크루명은 필수입니다.")
    @Size(max = 100, message = "크루명은 100자 이내로 입력해주세요.")
    private String crewName;

    @Schema(description = "크루 소개글")
    @Size(max = 500, message = "크루 소개글은 500자 이내로 입력해주세요.")
    private String crewDescription;

    @Schema(description = "크루 이미지 URL")
    private String crewImageUrl;

    @Schema(description = "지역", required = true)
    @NotBlank(message = "지역은 필수입니다.")
    @Size(max = 100, message = "지역은 100자 이내로 입력해주세요.")
    private String region;

    @Schema(description = "러닝 거리", required = true)
    @NotBlank(message = "러닝 거리는 필수입니다.")
    private String distance;

    @Schema(description = "평균 페이스", required = true)
    @NotBlank(message = "평균 페이스는 필수입니다.")
    private String averagePace;

    @Schema(description = "정기모임일시", required = true)
    @NotBlank(message = "정기모임일시는 필수입니다.")
    @Size(max = 100, message = "정기모임일시는 100자 이내로 입력해주세요.")
    private String activityTime;

}

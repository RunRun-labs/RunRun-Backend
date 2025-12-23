package com.multi.runrunbackend.domain.crew.dto.req;

import com.multi.runrunbackend.domain.crew.constant.CrewDistanceType;
import com.multi.runrunbackend.domain.crew.constant.CrewPaceType;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 요청 DTO
 * @filename : CrewJoinReqDto
 * @since : 25. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 가입 신청 요청 DTO")
public class CrewJoinReqDto {

    @Schema(description = "자기소개 (최대 100자)")
    private String introduction;

    @NotNull(message = "희망 러닝 거리는 필수입니다.")
    @Schema(description = "희망 러닝 거리 (km)", required = true)
    private CrewDistanceType distance;

    @NotNull(message = "평균 페이스는 필수입니다.")
    @Schema(description = "평균 페이스 (분/km)", required = true)
    private CrewPaceType pace;

    @NotBlank(message = "선호 지역은 필수입니다.")
    @Schema(description = "선호 지역 (구까지)", required = true)
    private String region;

    /**
     * @param crew 신청할 크루 엔티티
     * @param user 신청하는 회원 엔티티
     * @description : 크루 가입 신청 DTO를 CrewJoinRequest 엔티티로 변환
     */
    public CrewJoinRequest toEntity(Crew crew, User user) {
        return CrewJoinRequest.toEntity(
                crew,
                user,
                this.introduction,
                this.distance,
                this.pace,
                this.region
        );
    }

}

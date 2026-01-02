package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewRecruitStatus;
import com.multi.runrunbackend.domain.crew.constant.CrewStatus;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 목록 조회 응답 DTO
 * @filename : CrewListResDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 목록 응답")
public class CrewListResDto {

    @Schema(description = "크루 ID")
    private Long crewId;

    @Schema(description = "크루명")
    private String crewName;

    @Schema(description = "크루 이미지 URL")
    private String crewImageUrl;

    @Schema(description = "지역")
    private String region;

    @Schema(description = "현재 크루원 수")
    private Long memberCount;

    @Schema(description = "모집 상태")
    private CrewRecruitStatus crewRecruitStatus;

    @Schema(description = "크루 상태")
    private CrewStatus crewStatus;

    @Schema(description = "러닝 거리")
    private String distance;

    @Schema(description = "평균 페이스")
    private String averagePace;

    /**
     * @param crew        크루 엔티티
     * @param memberCount 크루원 수
     * @description : fromEntity : Entity → DTO 변환
     */
    public static CrewListResDto fromEntity(Crew crew, Long memberCount, String httpsImageUrl) {
        return CrewListResDto.builder()
                .crewId(crew.getId())
                .crewName(crew.getCrewName())
                .crewImageUrl(httpsImageUrl)
                .region(crew.getRegion())
                .memberCount(memberCount)
                .crewRecruitStatus(crew.getCrewRecruitStatus())
                .crewStatus(crew.getCrewStatus())
                .distance(crew.getDistance())
                .averagePace(crew.getAveragePace())
                .build();
    }
}

package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewRecruitStatus;
import com.multi.runrunbackend.domain.crew.constant.CrewStatus;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 상세 조회 응답 DTO
 * @filename : CrewDetailResDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 상세 조회 응답")
public class CrewDetailResDto {

    @Schema(description = "크루 ID")
    private Long crewId;

    @Schema(description = "크루명")
    private String crewName;

    @Schema(description = "크루장 이름")
    private String leaderNm;

    @Schema(description = "지역")
    private String region;

    @Schema(description = "현재 크루원 수")
    private Long memberCount;

    @Schema(description = "크루 소개글")
    private String crewDescription;

    @Schema(description = "정기모임일시")
    private String regularMeetingTime;

    @Schema(description = "모집 상태")
    private CrewRecruitStatus crewRecruitStatus;

    @Schema(description = "크루 상태")
    private CrewStatus crewStatus;

    @Schema(description = "크루 이미지 URL")
    private String crewImageUrl;

    @Schema(description = "러닝 거리")
    private String distance;

    @Schema(description = "평균 페이스")
    private String averagePace;

    @Schema(description = "최근 활동 내역 (최대 5개)")
    private List<CrewActivityResDto> recentActivities;

    /**
     * @param crew             크루 엔티티
     * @param memberCount      크루원 수
     * @param recentActivities 최근 활동 내역
     * @description : formEntity : Entity → DTO 변환
     */
    public static CrewDetailResDto fromEntity(
            Crew crew,
            Long memberCount,
            List<CrewActivityResDto> recentActivities,
            String httpsImageUrl
    ) {
        return CrewDetailResDto.builder()
                .crewId(crew.getId())
                .crewName(crew.getCrewName())
                .leaderNm(crew.getUser() != null ? crew.getUser().getName() : null)
                .region(crew.getRegion())
                .memberCount(memberCount)
                .crewDescription(crew.getCrewDescription())
                .regularMeetingTime(crew.getActivityTime())
                .crewRecruitStatus(crew.getCrewRecruitStatus())
                .crewStatus(crew.getCrewStatus())
                .crewImageUrl(httpsImageUrl)
                .distance(crew.getDistance())
                .averagePace(crew.getAveragePace())
                .recentActivities(recentActivities)
                .build();
    }
}

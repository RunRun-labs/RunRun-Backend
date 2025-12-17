package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.entity.CrewActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 크루 최근 활동 응답 DTO
 * @filename : CrewActivityResDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 최근 활동 응답")
public class CrewActivityResDto {

    @Schema(description = "활동 ID")
    private Long activityId;

    @Schema(description = "활동명")
    private String activityName;

    @Schema(description = "활동일")
    private LocalDateTime activityDate;

    @Schema(description = "참여 인원 수")
    private Integer participantCount;

    @Schema(description = "장소")
    private String location;

    @Schema(description = "거리")
    private Integer distance;

    /**
     * @param crewActivity 크루 활동 엔티티
     * @description : toEntity : Entity → DTO 변환
     */
    public static CrewActivityResDto toEntity(CrewActivity crewActivity) {
        String activityName = crewActivity.getRegion() + " "
                + crewActivity.getDistance() + "km 러닝";

        return CrewActivityResDto.builder()
                .activityId(crewActivity.getId())
                .activityName(activityName)
                .activityDate(crewActivity.getCreatedAt())
                .participantCount(crewActivity.getParticipationCnt())
                .location(crewActivity.getRegion())
                .distance(crewActivity.getDistance())
                .build();
    }
}

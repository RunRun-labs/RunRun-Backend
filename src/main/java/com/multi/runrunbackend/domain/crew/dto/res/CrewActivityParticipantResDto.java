package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.entity.CrewActivityUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 활동 참여자 응답 DTO
 * @filename : CrewActivityParticipantResDto
 * @since : 25. 1. 11. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 활동 참여자 정보")
public class CrewActivityParticipantResDto {

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "사용자명")
    private String userName;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    public static CrewActivityParticipantResDto fromEntity(CrewActivityUser activityUser) {
        return CrewActivityParticipantResDto.builder()
                .userId(activityUser.getUser().getId())
                .userName(activityUser.getUser().getName())
                .profileImageUrl(activityUser.getUser().getProfileImageUrl())
                .build();
    }
}

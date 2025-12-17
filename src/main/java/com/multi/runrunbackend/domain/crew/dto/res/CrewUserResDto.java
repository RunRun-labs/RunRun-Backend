package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.entity.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 크루원 정보 응답 DTO
 * @filename : CrewUserResDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루원 정보 응답")
public class CrewUserResDto {

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "사용자명")
    private String userName;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "역할")
    private CrewRole role;

    @Schema(description = "가입일")
    private LocalDateTime createdAt;

    @Schema(description = "참여 횟수")
    private Integer participationCount;

    /**
     * @param crewUser 크루원 엔티티
     * @description : toEntity : Entity → DTO 변환
     */
    public static CrewUserResDto toEntity(CrewUser crewUser) {
        return CrewUserResDto.builder()
                .userId(crewUser.getUser().getId())
                .userName(crewUser.getUser().getName())
                .profileImageUrl(crewUser.getUser().getProfileImageUrl())
                .role(crewUser.getRole())
                .createdAt(crewUser.getCreatedAt())
                .participationCount(crewUser.getParticipationCount())
                .build();
    }
}

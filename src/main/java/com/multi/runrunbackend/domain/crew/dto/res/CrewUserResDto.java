package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewRole;
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

    @Schema(description = "참여 횟수")  // dto에 놓고 service에서 계산 예정
    private Integer participationCount;

    @Schema(description = "마지막 활동일")  // ex. 크루원 목록에서 마지막 활동 표시용
    private LocalDateTime lastActivityDate;

    @Schema(description = "멤버십 보유 여부 (크루장 위임 가능 여부 판단용)")
    private Boolean hasMembership;

    /**
     * @param crewUser           크루원 엔티티
     * @param participationCount 참여 횟수
     * @param lastActivityDate   마지막 활동일
     * @description : fromEntity : Entity → DTO 변환
     */
    public static CrewUserResDto fromEntity(CrewUser crewUser,
                                            Integer participationCount,
                                            LocalDateTime lastActivityDate,
                                            Boolean hasMembership) {

        return CrewUserResDto.builder()
                .userId(crewUser.getUser().getId())
                .userName(crewUser.getUser().getName())
                .profileImageUrl(crewUser.getUser().getProfileImageUrl())
                .role(crewUser.getRole())
                .createdAt(crewUser.getCreatedAt())
                .participationCount(participationCount)
                .lastActivityDate(lastActivityDate)
                .hasMembership(hasMembership)
                .build();
    }
}

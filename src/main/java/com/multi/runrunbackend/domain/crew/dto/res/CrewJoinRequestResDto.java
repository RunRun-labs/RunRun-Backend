package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewDistanceType;
import com.multi.runrunbackend.domain.crew.constant.CrewPaceType;
import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 목록 조회 응답 DTO
 * @filename : CrewJoinRequestResDto
 * @since : 25. 12. 22. 월요일
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 가입 신청 목록 조회 응답 DTO")
public class CrewJoinRequestResDto {
    @Schema(description = "가입 신청 ID")
    private Long joinRequestId;

    @Schema(description = "신청자 회원 ID")
    private Long userId;

    @Schema(description = "신청자 이름")
    private String userName;

    @Schema(description = "신청자 프로필 이미지 URL")
    private String userProfileImageUrl;

    @Schema(description = "자기소개")
    private String introduction;

    @Schema(description = "희망 러닝 거리 (km)")
    private CrewDistanceType distance;

    @Schema(description = "평균 페이스 (분/km)")
    private CrewPaceType pace;

    @Schema(description = "선호 지역")
    private String region;

    @Schema(description = "신청 상태 (PENDING, APPROVED, REJECTED, CANCELED)")
    private JoinStatus joinStatus;

    @Schema(description = "신청 일시")
    private LocalDateTime createdAt;

    /**
     * @param joinRequest 크루 가입 신청 엔티티
     * @description : toDto : Entity → DTO 변환
     */
    public static CrewJoinRequestResDto toDto(CrewJoinRequest joinRequest) {
        return CrewJoinRequestResDto.builder()
                .joinRequestId(joinRequest.getId())  // 실무에선 둘 다 포함(안전한 식별)
                .userId(joinRequest.getUser().getId())
                .userName(joinRequest.getUser().getName())
                .userProfileImageUrl(joinRequest.getUser().getProfileImageUrl())
                .introduction(joinRequest.getIntroduction())
                .distance(joinRequest.getDistance())
                .pace(joinRequest.getPace())
                .region(joinRequest.getRegion())
                .joinStatus(joinRequest.getJoinStatus())
                .createdAt(joinRequest.getCreatedAt())
                .build();
    }
}

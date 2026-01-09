package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.constant.Tier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : 매칭 대기방 참가자 정보
 * @filename : MatchWaitingParticipantDto
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchWaitingParticipantDto {
    
    private Long userId;
    private String name;
    private String profileImage;
    private Boolean isReady;
    private Boolean isHost;  // 방장 여부
    private Tier tier;  // 티어 정보
    private String avgPace;  // 평균 페이스 ("MM:SS" 형식, User.averagePace에서 계산)
}

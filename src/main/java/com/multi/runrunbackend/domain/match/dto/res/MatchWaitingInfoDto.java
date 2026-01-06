package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : chang
 * @description : 매칭 대기방 세션 정보
 * @filename : MatchWaitingInfoDto
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchWaitingInfoDto {
    
    private Long sessionId;
    private Double targetDistance;  // 미터 단위
    private SessionStatus status;
    private LocalDateTime createdAt;
    private Long remainingSeconds;  // 남은 시간 (초) - 추가
    
    private List<MatchWaitingParticipantDto> participants;
    private Integer readyCount;  // 준비 완료한 인원
    private Integer totalCount;  // 전체 인원
}

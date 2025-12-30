package com.multi.runrunbackend.domain.running.battle.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : chang
 * @description : 배틀 GPS 전송 요청 DTO
 * @filename : BattleGpsRequest
 * @since : 2025-12-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleGpsRequest {
    
    private Long sessionId;
    private Long userId;  // 추가
    private Double totalDistance;  // 클라이언트가 계산한 누적 거리 (미터)
    private GpsData gps;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsData {
        private Double lat;
        private Double lng;
        private String timestamp;
        private Double speed;  // m/s (선택사항)
    }
}

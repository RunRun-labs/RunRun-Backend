package com.multi.runrunbackend.domain.running.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * GPS 데이터 전송 DTO
 * - 클라이언트 → 서버 (WebSocket)
 * - Redis 임시 저장용
 * 
 * @author : chang
 * @since : 2024-12-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)  // Redis @class 필드 무시
public class GPSDataDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 세션 정보
    @JsonProperty("sessionId")
    private Long sessionId;
    
    @JsonProperty("userId")
    private Long userId;
    
    // GPS 좌표
    @JsonProperty("latitude")
    private Double latitude;   // 위도
    
    @JsonProperty("longitude")
    private Double longitude;  // 경도
    
    @JsonProperty("accuracy")
    private Double accuracy;   // 정확도 (미터)
    
    // 시간
    @JsonProperty("timestamp")
    private Long timestamp;    // GPS 수신 시간 (epoch millis)
    
    // 런닝 데이터 (클라이언트에서 계산)
    @JsonProperty("totalDistance")
    private Double totalDistance;  // 총 거리 (km)
    
    @JsonProperty("runningTime")
    private Integer runningTime;   // 런닝 시간 (초)
    
    @JsonProperty("speed")
    private Double speed;          // 속도 (m/s)
}

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

    // 진행 방향 (degrees, 0=N, 90=E) - 지원 안 되는 기기/브라우저에서는 null 가능
    @JsonProperty("heading")
    private Double heading;

    /**
     * 코스 위 매칭 진행도 (미터)
     * - 방장 프론트에서 "코스 선 위를 지나갈 때만" 계산한 누적 진행도
     * - 참여자 화면에서 동일한 기준으로 선을 지우기 위해 브로드캐스트에 포함할 수 있음
     */
    @JsonProperty("matchedDistanceM")
    private Double matchedDistanceM;
}

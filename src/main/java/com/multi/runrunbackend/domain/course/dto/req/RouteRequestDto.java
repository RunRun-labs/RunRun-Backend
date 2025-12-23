package com.multi.runrunbackend.domain.course.dto.req;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : RouteRequest
 * @since : 2025. 12. 18. Thursday
 */

@Getter
@Setter
public class RouteRequestDto {

    private double startLat;
    private double startLng;

    // 편도에서만 사용
    private Double endLat;
    private Double endLng;

    // 왕복에서 사용 (km)
    private Double distanceKm;

    // 자동 생성 시 사용 (전체 경로 포인트 리스트)
    private List<RoutePointDto> viaPoints;
}

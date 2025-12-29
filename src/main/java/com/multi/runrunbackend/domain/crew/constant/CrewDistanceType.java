package com.multi.runrunbackend.domain.crew.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 관련 거리정보 ENUM (3km 미만, 3km, 5km, 10km, 10km 이상)
 * @filename : CrewDistanceType
 * @since : 2025-12-22 월요일
 */
@Getter
@RequiredArgsConstructor
public enum CrewDistanceType {
    UNDER_3KM("3km 미만"),
    KM_3("3km"),
    KM_5("5km"),
    KM_10("10km"),
    OVER_10KM("10km 초과");

    private final String description;
}

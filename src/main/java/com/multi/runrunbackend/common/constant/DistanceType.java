package com.multi.runrunbackend.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : chang
 * @description : 거리정보 ENUM (3km,5km,10km)
 * @filename : DistanceType
 * @since : 2025-12-17 수요일
 */
@Getter
@RequiredArgsConstructor
public enum DistanceType {
    KM_3("3km"),
    KM_5("5km"),
    KM_10("10km");

    private final String description;
}

package com.multi.runrunbackend.domain.crew.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 관련 페이스 정보 ENUM (2분/km 이하, 3분/km, 5분/km, 10km, 10km 이상)
 * @filename : CrewPaceType
 * @since : 2025-12-22 월요일
 */
@Getter
@RequiredArgsConstructor
public enum CrewPaceType {
    UNDER_3_MIN("2분/km 이하"),
    MIN_3_TO_4("3~4분/km"),
    MIN_5_TO_6("5~6분/km"),
    MIN_7_TO_8("7~8분/km"),
    OVER_9_MIN("9분/km 이상");

    private final String description;
}

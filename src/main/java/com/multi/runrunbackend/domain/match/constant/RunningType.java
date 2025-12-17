package com.multi.runrunbackend.domain.match.constant;

/**
 * @description :런닝 타입 ENUM(오프라인,온라인배틀,고스트런,솔로)
 * @author : chang
 * @filename : RunningType
 * @since : 2025-12-17 수요일
 */

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RunningType {
    SOLO("솔로"),
    OFFLINE("오프라인"),
    ONLINEBATTLE("온라인배틀"),
    GHOST("고스트");

    private final String description;
}

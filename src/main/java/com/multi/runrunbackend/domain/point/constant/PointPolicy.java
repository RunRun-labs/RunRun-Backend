package com.multi.runrunbackend.domain.point.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : BoKyung
 * @description : 적립/사용되는 포인트 관리하는 enum
 * @filename : PointPolicy
 * @since : 26. 01. 09. 금요일
 */
@Getter
@RequiredArgsConstructor
public enum PointPolicy {

    // 포인트 차감
    CREW_JOIN(100, "크루 가입", PointType.USE),

    // 포인트 적립
    RUNNING_COMPLETE(1, "러닝 완주 (100m당)", PointType.EARN),
    ATTENDANCE(50, "출석", PointType.EARN),
    CHALLENGE_SUCCESS(100, "챌린지 성공", PointType.EARN);

    private final int points;
    private final String description;
    private final PointType type;     // 적립/차감 구분

    /**
     * 포인트 타입 (적립/차감)
     */
    public enum PointType {
        EARN,
        USE
    }
}
